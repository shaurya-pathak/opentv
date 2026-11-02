package app.opentv

import app.opentv.control.ControlServer
import app.opentv.control.PlaybackControlSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.json.JSONObject

@RunWith(RobolectricTestRunner::class)
class ControlStatusTest {
    @Test fun statusUsesStableSnakeCaseAndContainsNoDisplayTitleOrUrl() {
        val json = JSONObject(
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
        )

        assertEquals("movie", json.getString("kind"))
        assertEquals(12L, json.getLong("source_id"))
        assertEquals("810", json.getString("provider_id"))
        assertEquals("playing", json.getString("state"))
        assertEquals(2000L, json.getLong("position_ms"))
        assertEquals(50, json.getInt("video_frames"))
        assertEquals("en-US", json.getString("audio_language"))
        assertEquals("selected", json.getString("english_status"))
        assertFalse(json.has("title"))
        assertFalse(json.has("url"))
    }
}
