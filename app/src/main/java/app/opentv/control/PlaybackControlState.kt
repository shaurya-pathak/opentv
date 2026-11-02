/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv.control

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlaybackControlSnapshot(
    val kind: String = "none",
    val sourceId: Long = 0,
    val providerId: String = "",
    val state: String = "idle",
    val positionMs: Long = 0,
    val videoFrames: Int? = null,
    val audioLanguage: String? = null,
    val englishStatus: String = "unknown",
    val recoveryExhausted: Boolean = false,
    val selectionError: String? = null,
)

/** In-process bridge between the player and the authenticated LAN control server. */
object PlaybackControlState {
    private val mutable = MutableStateFlow(PlaybackControlSnapshot())
    val snapshot = mutable.asStateFlow()
    private var stopSequence = 0L
    private val mutableStopSequence = MutableStateFlow(0L)
    val stopSequenceFlow = mutableStopSequence.asStateFlow()

    @Synchronized
    fun expect(kind: String, sourceId: Long, providerId: String) {
        mutable.value = PlaybackControlSnapshot(
            kind = kind,
            sourceId = sourceId,
            providerId = providerId,
            state = "buffering",
            englishStatus = if (kind == "movie") "pending" else "unknown",
        )
    }

    @Synchronized
    fun selectionError(kind: String, sourceId: Long, providerId: String, code: String) {
        mutable.value = PlaybackControlSnapshot(
            kind = kind,
            sourceId = sourceId,
            providerId = providerId,
            state = "error",
            selectionError = code,
            englishStatus = if (kind == "movie") "pending" else "unknown",
        )
    }

    @Synchronized
    fun update(
        kind: String,
        sourceId: Long,
        providerId: String,
        state: String,
        positionMs: Long,
        videoFrames: Int?,
        audioLanguage: String?,
        englishStatus: String,
        recoveryExhausted: Boolean = false,
    ) {
        mutable.value = PlaybackControlSnapshot(
            kind, sourceId, providerId, state, positionMs.coerceAtLeast(0), videoFrames,
            audioLanguage, englishStatus, recoveryExhausted,
        )
    }

    @Synchronized
    fun stop() {
        stopSequence++
        mutableStopSequence.value = stopSequence
    }

    @Synchronized
    fun idleIfCurrent(kind: String, sourceId: Long, providerId: String) {
        val old = mutable.value
        if (old.kind == kind && old.sourceId == sourceId && old.providerId == providerId) {
            mutable.value = old.copy(state = "idle", positionMs = 0, videoFrames = null, recoveryExhausted = false)
        }
    }
}
