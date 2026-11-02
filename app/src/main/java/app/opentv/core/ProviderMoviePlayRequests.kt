/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A one-shot provider movie key; provider IDs are resolved against this install's catalog. */
object ProviderMoviePlayRequests {
    private val pending = MutableStateFlow<ChannelLink.ProviderMovieTarget?>(null)
    val target = pending.asStateFlow()
    fun request(target: ChannelLink.ProviderMovieTarget) { pending.value = target }
    fun consume() { pending.value = null }
}
