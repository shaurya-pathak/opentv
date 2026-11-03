/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv.core

import android.view.Window
import android.view.WindowManager
import java.util.WeakHashMap

/**
 * Shares the Activity window's [WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON] between screens.
 *
 * Compose Navigation keeps the outgoing screen composed during a transition. If that screen clears
 * the flag in `onDispose` — as the home screen did when leaving for the player — it turns the
 * screen back off just after the incoming player turned it on. Holding is reference-counted per
 * screen instance, so the flag is only cleared when the last holder lets go.
 */
object ScreenAwake {

    private val holders = WeakHashMap<Window, MutableSet<Any>>()

    fun acquire(window: Window?, holder: Any) {
        if (window == null) return
        holders.getOrPut(window) { mutableSetOf() }.add(holder)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    fun release(window: Window?, holder: Any) {
        if (window == null) return
        val set = holders[window] ?: return
        set.remove(holder)
        if (set.isEmpty()) {
            holders.remove(window)
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}
