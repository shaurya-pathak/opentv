/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Resolve the provider key against this installation, rather than guessing a Room ID. */
object ProviderPlayRequests {
    private val pending = MutableStateFlow<ChannelLink.ProviderTarget?>(null)
    val target = pending.asStateFlow()
    fun request(target: ChannelLink.ProviderTarget) { pending.value = target }
    fun consume() { pending.value = null }
}
