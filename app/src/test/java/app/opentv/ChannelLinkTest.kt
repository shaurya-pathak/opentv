package app.opentv

import app.opentv.core.ChannelLink
import org.junit.Assert.*
import org.junit.Test

class ChannelLinkTest {
    @Test fun acceptsProviderKeyWithoutConfusingItWithLocalChannelId() {
        val target = ChannelLink.providerTarget("opentv-lab://source/2/stream/100", "opentv-lab")
        assertEquals(2L, target?.sourceId)
        assertEquals("100", target?.streamId)
        assertNull(ChannelLink.channelId("opentv-lab://source/2/stream/100", "opentv-lab"))
    }
    @Test fun rejectsInvalidProviderSourcesAndStreamIds() {
        listOf("opentv-lab://source/0/stream/100", "opentv-lab://source/2/stream/-1",
            "opentv-lab://source/2/stream/01", "opentv-lab://source/2/stream/100?url=secret",
            "opentv-lab://source/2/stream/9223372036854775808", "opentv-local://source/2/stream/100")
            .forEach { assertNull(it, ChannelLink.providerTarget(it, "opentv-lab")) }
    }
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
