package app.opentv

import app.opentv.player.PlaybackLease
import org.junit.Assert.*
import org.junit.Test

class PlaybackLeaseTest {
    @Test fun handoffStopsOldPlaybackAndOldReleaseCannotRevokeNewOwner() {
        val lease = PlaybackLease(); val old = Any(); val next = Any(); val third = Any()
        var oldStopped = 0; var nextStopped = 0
        lease.claim(old) { oldStopped++; lease.release(old) }
        lease.claim(next) { nextStopped++; lease.release(next) }
        assertEquals(1, oldStopped)
        lease.release(old)
        lease.claim(third) {}
        assertEquals(1, nextStopped)
    }
    @Test fun retryBySameOwnerDoesNotStopItselfAndReleasedOwnerIsNotStoppedAgain() {
        val lease = PlaybackLease(); val owner = Any(); var stopped = 0
        lease.claim(owner) { stopped++ }
        lease.claim(owner) { stopped++ }
        assertEquals(0, stopped)
        lease.release(owner)
        lease.claim(Any()) {}
        assertEquals(0, stopped)
    }
}
