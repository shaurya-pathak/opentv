package app.opentv

import app.opentv.player.isKnownNonEnglishAudioLanguage
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AudioLanguageTest {
    @Test fun onlyIdentifiedNonEnglishLanguagesCountAsEnglishUnavailable() {
        assertThat("fr".isKnownNonEnglishAudioLanguage()).isTrue()
        assertThat("es-MX".isKnownNonEnglishAudioLanguage()).isTrue()
        assertThat("en".isKnownNonEnglishAudioLanguage()).isFalse()
        assertThat("eng".isKnownNonEnglishAudioLanguage()).isFalse()
        assertThat((null as String?).isKnownNonEnglishAudioLanguage()).isFalse()
        assertThat("und".isKnownNonEnglishAudioLanguage()).isFalse()
        assertThat("mul".isKnownNonEnglishAudioLanguage()).isFalse()
        assertThat("zxx".isKnownNonEnglishAudioLanguage()).isFalse()
    }
}
