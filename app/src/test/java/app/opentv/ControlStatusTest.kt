package app.opentv

import app.opentv.control.ControlServer
import app.opentv.control.PlaybackControlSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ControlStatusTest {
    @Test fun statusUsesStableSnakeCaseAndContainsNoDisplayTitleOrUrl() {
        val json = Json.parseToJsonElement(
            ControlServer.statusJson(
                PlaybackControlSnapshot(
                    kind = "movie",
                    sourceId = 12,
                    providerId = "810",
                    state = "playing",
                    positionMs = 2000,
                    videoFrames = 50,
                    audioLanguage = "en-US",
                    englishStatus = "selected",
                ),
            ),
        ).jsonObject

        assertEquals("movie", json.getValue("kind").jsonPrimitive.content)
        assertEquals(12L, json.getValue("source_id").jsonPrimitive.content.toLong())
        assertEquals("810", json.getValue("provider_id").jsonPrimitive.content)
        assertEquals("playing", json.getValue("state").jsonPrimitive.content)
        assertEquals(2000L, json.getValue("position_ms").jsonPrimitive.content.toLong())
        assertEquals(50, json.getValue("video_frames").jsonPrimitive.content.toInt())
        assertEquals("en-US", json.getValue("audio_language").jsonPrimitive.content)
        assertEquals("selected", json.getValue("english_status").jsonPrimitive.content)
        assertEquals("false", json.getValue("recovery_exhausted").jsonPrimitive.content)
        assertEquals("null", json.getValue("selection_error").toString())
        assertFalse("title" in json)
        assertFalse("url" in json)
    }
}
