/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv.player

/** Main-thread handoff: navigation animations must not overlap two live players. */
internal class PlaybackLease {
    private var owner: Any? = null
    private var stopOwner: (() -> Unit)? = null

    fun claim(next: Any, stop: () -> Unit) {
        if (owner === next) return
        val previous = stopOwner
        owner = next
        stopOwner = stop
        // Set the new owner first: releasing the old one cannot revoke the new lease.
        previous?.invoke()
    }

    fun release(previous: Any) {
        if (owner === previous) { owner = null; stopOwner = null }
    }
}
