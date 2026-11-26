# TOVI

**Time, on your terms.**

TOVI is a digital-wellbeing project built around a simple idea: your attention belongs to you. The Android app is being developed to help people set intentional limits around selected apps, understand their usage, and make more room for life beyond the screen.

 > **Development status:** active Android development. First-run onboarding, selected-app monitoring, foreground-time accounting, daily token reset, an allowance-aware intervention with a configurable pause and breathing animation, a five-minute intentional continuation, a live Home summary, and seven-day usage insights are now implemented in the development branch on `main`. This is not yet a release: notifications, richer scheduling, data export, accessibility review, and real-device validation remain in progress.

## The TOVI approach

- **Your time, your rules** — choose a daily allowance that fits your life.
- **One minute at a time** — the planned token model makes selected-app usage understandable.
- **Helpful, not noisy** — reminders are designed around meaningful milestones, not constant interruptions.
- **No shame, no lectures** — TOVI is about intentional choices, not guilt.

## Product experience

The planned experience has four parts:

1. **Home** — see your daily allowance and a clear view of the day.
2. **Apps** — choose which apps count toward your allowance.
3. **Insights** — understand patterns over time.
4. **Settings** — set your allowance, reset time, and visual preferences.

### Token model

The intended model is **1 token = 1 minute** of foreground use in an app selected by the user. Background time should not consume tokens. The daily allowance and reset schedule will be configurable. When the allowance is exhausted, the planned experience will pause access to selected apps until the next reset.

### Milestone reminders

The planned reminder points are 25%, 50%, 75%, 90%, and 100% of the daily allowance. These are intended to be meaningful check-ins rather than recurring nags.

## Current implementation

| Capability | Status |
| --- | --- |
| Android project and Compose interface | Available |
| Daily allowance setting saved locally | Available |
| Accent colour selection saved locally | Available |
| Shortcut to Android usage-access settings | Available |
| Monitored-app selection | Available |
| Foreground usage tracking | Available; requires real-device validation |
| Token deduction and daily reset | Available |
| Milestone notifications | Recorded locally; notifications pending |
| Blocking when allowance is exhausted | Available; OEM validation pending |
| Usage insights and seven-day history | Available |
| Privacy, accessibility, and release review | Planned |

TOVI asks the user to enable its Accessibility service manually in Android Settings. The service detects foreground changes for selected apps, records foreground seconds locally, and converts each accumulated minute into one token. The pause overlay appears when a selected app opens. A configurable 3-, 5-, or 10-second pause precedes a five-minute continuation while tokens remain; the intervention includes a gentle breathing animation, and time on the pause screen itself is not counted. When the allowance reaches zero, the selected app is paused with no continue option until the next local-day reset. Home and Insights read the local usage ledger. The service does not inspect screen text, messages, photos, or keystrokes. Android OEM background restrictions and overlay behavior still require real-device validation.

## Technology

- Kotlin
- Jetpack Compose and Material 3
- Android minimum: 8.0 (API 26)
- Compile / target SDK: 35
- JDK: 17
- Gradle build through GitHub Actions

The app currently retains its original Android application ID, `com.limity.app`, so existing development installs can be updated without changing package identity during the brand transition. The visible product name is TOVI.

## Build the Android app

Open the repository in Android Studio and allow Gradle to sync. To build a debug APK locally:

```bash
gradle --no-daemon assembleDebug
```

Every push to `main` and manual workflow run triggers the Android build. Download the **TOVI-debug** artifact from the successful GitHub Actions run.

## Official website and downloads

The TOVI website is deployed with GitHub Pages:

- **Website:** https://iy367-ux.github.io/TOVI/
- **Releases:** https://github.com/iy367-ux/TOVI/releases
- **Source:** https://github.com/iy367-ux/TOVI

The website's download centre reads the public GitHub Releases feed. It shows published releases and APK assets only; it does not present unverified CI artifacts as stable downloads. Until an official release is published, the download centre will say that no release is available.

## Development roadmap

1. Implement reliable foreground-app usage tracking.
2. Build the token engine, daily reset, and app-selection experience.
3. Add milestone notifications and transparent, user-authorized blocking.
4. Test on real devices, including Android 16 and common OEM battery restrictions.
5. Complete privacy, accessibility, and distribution-policy reviews.
6. Prepare the first signed release, then evaluate the iOS approach using Apple's supported Screen Time APIs.

## Brand system

- **Name:** TOVI
- **Tagline:** Time, on your terms.
- **Primary type:** Manrope
- **Secondary type:** DM Sans
- **Midnight Forest:** `#101512`
- **Deep Sage:** `#1B2821`
- **Living Green:** `#83C99D`

---

*Made for a more intentional everyday.*

 
## Beyond the Android app

TOVI is being imagined as a small ecosystem, not just a phone app. The following projects are **planned and not released**:

- **TOVI Browser Extension & New Tab** — a browser companion with a highly customisable new-tab page, shortcuts, useful widgets and room for community-made themes. The goal is deep personalisation while keeping the interface calm and lightweight.
- **Community Themes** — a future way to create, share and install visual themes across supported TOVI experiences. Theme publishing and installation will need clear permissions, safe defaults and compatibility checks.
- **TOVI Kids Mode** — a distinct, more playful and age-appropriate experience, with friendly visuals, simple routines and parent-guided boundaries. It should feel welcoming and fun rather than like the standard TOVI interface with a different label.

These are roadmap items. They are not available for download or public use yet, and their designs and scope may change as they are developed.
