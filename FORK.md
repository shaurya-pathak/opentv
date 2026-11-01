# OpenTV Lab

Our working fork of [OpenTV](https://github.com/opentvproject/opentv), focused on
reliable live playback, observable player health, and configurable recovery.
The upstream history, copyright notices and GPL-3.0 license are preserved.

- Fork: https://github.com/shaurya-pathak/opentv
- Development branch: `codex/playback-recovery`
- Starting upstream commit: `443811904b6437720d37bb1064c0830906c3515f` (v0.11.10)
- Recovery design and regression scenarios: [PLAYBACK_RECOVERY.md](docs/PLAYBACK_RECOVERY.md)

This branch establishes the development setup. Recovery behavior has **not yet
been changed**. Creating the fork does not install or replace anything on the TV.

## Build and test

Requirements: JDK 17+ and Android SDK API 35. Set `ANDROID_HOME` or an ignored
`local.properties` containing `sdk.dir=/absolute/path/to/android/sdk`.

```sh
./gradlew testDebugUnitTest assembleDebug
```

Debug builds use package `app.opentv.debug`, display **OpenTV Lab**, and have a
`-lab` version suffix. They can coexist with the upstream `app.opentv` app and
have separate settings/data. The two apps must not play a one-connection provider
account simultaneously. Do not assume settings or channel database IDs transfer
between them.

The Build workflow runs tests and assembles a debug APK on `main`, `codex/**`
branches, pull requests and manual dispatch. Download `opentv-debug-apk` from a
successful run; the APK is a development artifact, not a production release.
No release or website publication is needed for development.

## Keep in sync

`origin` is our fork; `upstream` is `opentvproject/opentv`.

```sh
git fetch upstream
git switch main
git merge --ff-only upstream/main
git push origin main
git switch codex/playback-recovery
# Review the upstream changes, then merge upstream/main when appropriate.
```

Commit source, synthetic fixtures and redacted findings only. Keep provider
credentials, authenticated stream URLs, account identifiers, raw device logs,
signing keys and personal channel preferences out of commits and CI artifacts.

The existing automation service remains in the sibling `smart-tv/` checkout.
It is not copied into this public fork and is not required by upstream OpenTV.
