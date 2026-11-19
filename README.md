# Netfly HDR

A cinematic Android TV streaming app built on [NuvioTV](https://github.com/NuvioMedia/NuvioTV)
(1.2.0-beta.2), with Netfly's red themes, home layouts, NavRail, provider hub,
profiles and ambient mode — rebuilt for Android from the Netfly design
reference (a Roku port of Nuvio). No Netfly code or assets are copied: the
design reference is the brief, the code is new work on the NuvioTV base.

- **Package:** `com.netfly.hdr` · **Version:** 1.0.0 (vc 1)
- **License:** GPL-3.0 (as NuvioTV)

## Build

```bash
# local.properties needs the Nuvio backend config + release signing, as in
# local.example.properties. CI builds run on push to main via
# .github/workflows/build-netfly-hdr.yml and upload signed Full release APKs
# to a draft release.
./gradlew :app:assembleFullRelease
```

The deliverable APK for the 32-bit box is
`app-full-armeabi-v7a-release.apk`.
