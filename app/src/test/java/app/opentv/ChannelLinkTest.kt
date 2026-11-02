package app.opentv

import app.opentv.core.ChannelLink
import org.junit.Assert.*
import org.junit.Test

class ChannelLinkTest {
    @Test fun acceptsLocalPositiveId() {
        assertEquals(42L, ChannelLink.channelId("opentv-lab://channel/42", "opentv-lab"))
    }

    @Test fun rejectsForeignOrAmbiguousInputs() {
        listOf(null, "https://channel/42", "opentv-local://channel/42",
            "opentv-lab://user@channel/42", "opentv-lab://channel:80/42",
            "opentv-lab://channel/0", "opentv-lab://channel/-1", "opentv-lab://channel/01",
            "opentv-lab://channel/%34%32", "opentv-lab://channel/42?url=secret",
            "opentv-lab://channel/42#fragment", "opentv-lab://channel/9223372036854775808",
            "opentv-lab://channel/42/extra").forEach {
            assertNull(it, ChannelLink.channelId(it, "opentv-lab"))
        }
    }
}
