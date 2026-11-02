/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv.core

import java.net.URI

/** Local catalog identifiers only: never accept provider URLs or credentials in a link. */
object ChannelLink {
    fun channelId(value: String?, scheme: String): Long? {
        if (value == null || value.length > 128) return null
        val uri = runCatching { URI(value) }.getOrNull() ?: return null
        if (uri.scheme != scheme || uri.rawAuthority != "channel" ||
            uri.rawQuery != null || uri.rawFragment != null) return null
        val path = uri.rawPath ?: return null
        if (!path.matches(Regex("/[1-9][0-9]{0,18}"))) return null
        return path.drop(1).toLongOrNull()?.takeIf { it > 0 }
    }
}
