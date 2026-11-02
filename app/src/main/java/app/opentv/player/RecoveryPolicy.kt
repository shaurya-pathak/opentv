/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv.player

/** Monotonic-clock policy, independent of Android so failure sequences can be tested. */
internal class RecoveryPolicy {
    var attempts = 0
        private set
    private var progressAt = 0L
    private var videoAt = 0L
    private var healthySince: Long? = null
    private var position: Long? = null
    private var frames: Int? = null
    private val audioErrors = ArrayDeque<Long>()

    fun newTune(now: Long) { attempts = 0; restarted(now) }

    fun restarted(now: Long) {
        progressAt = now; videoAt = now; healthySince = null
        position = null; frames = null; audioErrors.clear()
    }

    fun reserveRestart(): Long? {
        if (attempts >= 3) return null
        attempts++
        return 1_500L * (1L shl (attempts - 1))
    }

    fun audioError(now: Long) { audioErrors.addLast(now); prune(now) }

    private fun prune(now: Long) {
        while (audioErrors.isNotEmpty() && now - audioErrors.first() > 10_000L) audioErrors.removeFirst()
        // Only a bounded recent burst is needed, even with a broken source producing many errors.
        while (audioErrors.size > 100) audioErrors.removeFirst()
    }

    fun sample(now: Long, wantsPlayback: Boolean, ready: Boolean, positionMs: Long,
               videoFrames: Int?, videoExpected: Boolean): String? {
        if (!wantsPlayback) { restarted(now); return null }
        prune(now)
        val advanced = position?.let { positionMs > it } ?: false
        val videoAdvanced = videoFrames != null && frames != null && videoFrames > frames!!
        if (advanced) progressAt = now
        if (videoAdvanced || !videoExpected) videoAt = now
        position = positionMs; frames = videoFrames
        if (ready && advanced && (!videoExpected || videoAdvanced) && audioErrors.isEmpty()) {
            if (healthySince == null) healthySince = now
            if (now - healthySince!! >= 60_000L) attempts = 0
        } else healthySince = null
        return when {
            audioErrors.size >= 20 && now - audioErrors.first() >= 8_000L ->
                "The stream has repeated audio timing errors."
            now - progressAt >= 30_000L -> "The stream stopped progressing."
            videoExpected && now - videoAt >= 30_000L -> "The video stopped progressing."
            else -> null
        }
    }
}
