package app.opentv

import app.opentv.player.RecoveryPolicy
import org.junit.Assert.*
import org.junit.Test

class RecoveryPolicyTest {
    @Test fun repeatedReadyAndRestartDoNotRefillBudget() {
        val p = RecoveryPolicy(); p.newTune(0)
        repeat(3) {
            assertNotNull(p.reserveRestart()); p.restarted(0)
            p.sample(2000, true, true, 2000, 60, true)
        }
        assertNull(p.reserveRestart())
        p.newTune(0); assertNotNull(p.reserveRestart())
    }
    @Test fun onlySustainedHealthyOutputRefillsBudget() {
        val p = RecoveryPolicy(); p.newTune(0); p.reserveRestart()
        for (i in 0..32) p.sample(i * 2000L, true, true, i * 2000L, i * 60, true)
        assertEquals(0, p.attempts)
    }
    @Test fun detectsFrozenVideoEvenWhenPositionAdvances() {
        val p = RecoveryPolicy(); p.newTune(0)
        var reason: String? = null
        for (i in 0..16) reason = p.sample(i * 2000L, true, true, i * 2000L, 1, true)
        assertEquals("The video stopped progressing.", reason)
    }
    @Test fun pausedPlaybackAndNormalAudioOnlyPlaybackDoNotRestart() {
        val p = RecoveryPolicy(); p.newTune(0)
        assertNull(p.sample(100_000, false, true, 0, 0, true))
        for (i in 0..32) assertNull(p.sample(100_000 + i * 2000L, true, true, i * 2000L, null, false))
    }
    @Test fun isolatedCorrectedAudioErrorDoesNotRestart() {
        val p = RecoveryPolicy(); p.newTune(0); p.audioError(0)
        assertNull(p.sample(2000, true, true, 2000, 60, true))
        assertNull(p.sample(12000, true, true, 12000, 360, true))
    }
    @Test fun sustainedAudioErrorBurstCanTriggerBoundedRecovery() {
        val p = RecoveryPolicy(); p.newTune(0)
        for (i in 0..20) p.audioError(i * 400L)
        assertEquals("The stream has repeated audio timing errors.",
            p.sample(8000, true, true, 8000, 240, true))
    }
    @Test fun pausedOrBufferingPeriodsDoNotCountAsHealthy() {
        val p = RecoveryPolicy(); p.newTune(0); p.reserveRestart()
        for (i in 0..100) p.sample(i * 2000L, false, false, 0, null, false)
        assertEquals(1, p.attempts)
    }
}
