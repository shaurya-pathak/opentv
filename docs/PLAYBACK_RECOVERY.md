# Playback recovery work plan

Baseline: OpenTV v0.11.10, upstream commit
`443811904b6437720d37bb1064c0830906c3515f`, Media3 1.5.0.

## Failures to address

Two distinct failure modes were observed during live MPEG-TS playback on an
Android 14 ARMv7 TV:

1. Repeated nonfatal audio timestamp discontinuities, with visible/audible
   stuttering. Media3 dispatches these through `onAudioSinkError`; they need not
   reach `Player.Listener.onPlayerError`.
2. Playback stopping silently: no advancing video frames, an audio track in the
   stopped state and no active stream reported by the provider, while the player
   screen remained open. The available logs did not establish why it stopped.

A second player also stalled on one feed, so engine choice alone is not an
established solution. Source settings and alternative feeds matter too.

## Baseline code findings

In `app/src/main/java/app/opentv/player/PlayerController.kt`:

- `onPlayerError` retries by calling `play`, which resets `consecutiveFailures`.
  The automatic restart budget therefore does not reliably bound repeated failures.
- `STATE_READY` immediately marks playback as Playing and clears failures. It
  does not establish sustained audio/video output.
- No audio sink analytics listener or progress watchdog monitors the two failure
  modes above. `STATE_ENDED` simply marks the controller idle.
- Delayed recovery work is not held in its own cancellable job. Request equality
  is an insufficient guard against an old recovery acting on a later same-channel
  tune; stop/release must explicitly invalidate prior work.

Media3 already attempts to adjust timestamps; suppressing its logs is not a fix.
Increasing buffering alone does not repair malformed timestamps.

## Planned sequence

1. **Bounded recovery.** Separate manual tunes from automatic restarts; preserve
   attempts across retries and reset only after a meaningful healthy interval.
   Give each tune a generation ID and cancel recovery on switch, stop or release.
2. **Observe health.** Capture nonfatal timing-error bursts, actual audio/video
   progress, buffering and stream end events. Distinguish intentional pause,
   preview, live playback and VOD. Never retune for an isolated corrected timestamp.
3. **Per-source/channel policy.** Preserve User-Agent and container settings;
   configure a small ordered set of explicitly chosen equivalent feeds. Keep
   recovery bounded with backoff/cooldown and show a useful final error.
4. **Optional embedded engine.** Evaluate LibVLC as an in-app fallback while
   retaining OpenTV's browsing/remote UI. MPV is another candidate, not a verified
   Android implementation here. Validate ARMv7 support, hardware decoding, audio
   sync, memory and license obligations before adding an engine dependency.

Release the old stream before connecting another. A provider's one-stream limit
applies across previews, recording, fallback engines and other devices. A failed
feed must not produce parallel probes or an infinite restart loop.

## Regression checks before live testing

- Consecutive fatal failures exhaust the recovery budget.
- Repeated READY/error transitions do not reset that budget prematurely.
- One corrected timestamp does not restart otherwise healthy playback.
- A sustained unhealthy burst can trigger only bounded recovery.
- Pausing or turning off playback does not trigger a restart.
- Channel changes, stop and release invalidate pending recovery, including a
  later tune to the same channel.
- An intentional VOD ending does not trigger a live-stream recovery.
- Live stream termination produces recovery or a clear error instead of a frozen
  player screen.
- Switching feeds or engines preserves source headers and never overlaps streams.

Use synthetic media or fixtures for automated tests. Keep real provider URLs and
credentials out of source, diagnostics, test reports and published artifacts.
