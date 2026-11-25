# TOVI

**Time, on your terms.**

TOVI is an Android-first digital-wellbeing app built around a simple idea: your attention belongs to you. It helps you set intentional limits around selected apps, understand your usage, and make more room for life beyond the screen.

> **Development status:** active development. The Android project currently includes first-run setup, selected-app monitoring, local foreground-time accounting, a daily allowance, an accessibility-based pause overlay, a configurable pause, a five-minute continuation, a Home summary, and a seven-day usage view. These features are implemented in the development source but are **not yet validated across real devices or OEMs**. Do not treat current tracking or blocking as release-ready.

## The TOVI approach

- **Your time, your rules** — choose a daily allowance that fits your life.
- **One minute at a time** — one token represents one minute of selected-app use; the interface presents the allowance in minutes.
- **Helpful, not noisy** — meaningful check-ins should appear in the app, not as a stream of notifications.
- **No shame, no lectures** — TOVI is about intentional choices, not guilt.

## Product experience

The app is organised around four areas:

1. **Home** — see the time remaining in your daily allowance and a summary of selected-app use.
2. **My apps** — choose which installed apps count toward your allowance.
3. **Insights** — review recent usage patterns.
4. **Settings** — adjust your daily allowance and intervention pause.

### Allowance model

The intended model is **1 token = 1 minute** of foreground use in an app selected by the user. Background time should not consume the allowance. The daily allowance is configurable. When the allowance is exhausted, TOVI pauses selected apps until the next local-day reset.

### Milestone moments

Milestones are intended to be calm, in-app moments rather than push notifications. The proposed default check-ins are 50%, 75%, 90%, and 100% of the daily allowance, shown at an appropriate next interaction. Push notifications are not part of the current implementation and should remain optional if introduced later.

## Current implementation

| Capability | Status |
| --- | --- |
| Android project and Compose interface | Implemented |
| First-run setup and local allowance setting | Implemented |
| Selected-app picker | Implemented |
| Accessibility data disclosure and affirmative first-run consent | Implemented |
| Accessibility-based foreground app detection | Implemented; real-device validation pending |
| Foreground-time accounting | Implemented with local aggregate counters; reconciliation and edge-case testing pending |
| Daily allowance and local-day reset | Implemented; clock and timezone edge cases need testing |
| Pause overlay and five-minute continuation | Implemented; OEM and Android-version validation pending |
| Home summary and seven-day usage view | Implemented |
| Milestone moments and notifications | Not implemented |
| Data export, privacy controls and accessibility review | Planned |
| Signed release and release-policy review | Planned |

TOVI asks the user to enable its Accessibility service manually in Android Settings. The service listens for foreground window changes for the selected apps and displays its pause overlay. Its accessibility configuration has canRetrieveWindowContent set to false; it does not request access to window text or interactive content. TOVI does not read messages, photos, screen text or keystrokes.

Current usage totals are stored locally in SharedPreferences, grouped by local day and package. This is an early implementation, not yet the append-only session ledger or UsageStats reconciliation model needed for stronger recovery and clock-change handling. The service uses Android's accessibility overlay type, so behaviour must be tested on real devices, Android versions and OEM battery-management implementations before any reliability claim is made.

## Technology

- Kotlin
- Jetpack Compose and Material 3
- Android minimum: 8.0 (API 26)
- Compile / target SDK: 35
- JDK: 17
- Gradle build through GitHub Actions

The Android application ID remains com.limity.app during development so existing test installations can continue to receive updates. A final TOVI application ID should be chosen before the first public release; changing it later would make it a separate Android app and require users to reinstall.

The app does not currently need network access for its core functionality. The Android manifest does not declare the INTERNET permission.

## Build the Android app

Open the repository in Android Studio and allow Gradle to sync. To build a debug APK locally:

```bash
gradle --no-daemon assembleDebug
```

Every push to main and manual workflow run triggers the Android build. Pull requests targeting main also run the build. Download the **TOVI-debug** artifact from a successful GitHub Actions run.

## Official website and downloads

The TOVI website is deployed with GitHub Pages:

- **Website:** https://iy367-ux.github.io/TOVI/
- **Releases:** https://github.com/iy367-ux/TOVI/releases
- **Source:** https://github.com/iy367-ux/TOVI

The website's download centre reads the public GitHub Releases feed. It shows published releases and APK assets only; it does not present unverified CI artifacts as stable downloads. Until an official release is published, the download centre will say that no release is available.

## Development roadmap

1. Make foreground tracking resilient to screen-off, process death, midnight rollover, clock changes and OEM background restrictions.
2. Evaluate UsageStats reconciliation and move from aggregate counters to a recoverable session ledger.
3. Improve onboarding, permission explanations and the protection-status experience.
4. Refine the intervention, allowance-exhausted state and optional in-app milestones.
5. Complete accessibility, privacy and Android distribution-policy reviews.
6. Prepare a signed release, then evaluate the iOS approach using Apple's supported Screen Time APIs.

## Brand system

- **Name:** TOVI
- **Tagline:** Time, on your terms.
- **Forest charcoal:** #17211B
- **Muted sage:** #526B59
- **Warm orange accent:** #F0923F
- **Colour balance:** 60-30-10; orange is reserved for emphasis and actions.
- **Typography:** final bundled app font pairing is still to be decided.

---

*Made for a more intentional everyday.*
