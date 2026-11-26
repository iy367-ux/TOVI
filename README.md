# TOVI

**Time, on your terms.**

TOVI is a digital-wellbeing project built around a simple idea: your attention belongs to you. The Android app is being developed to help people set intentional limits around selected apps, understand their usage, and make more room for life beyond the screen.

> **Development status:** TOVI for Android is in active development. First-run onboarding, selected-app monitoring, foreground-time accounting, daily token reset, an allowance-aware intervention with a configurable pause and breathing animation, a five-minute intentional continuation, a live Home summary, and seven-day usage insights are implemented in the development branch on `main`. This is not yet a stable release: notifications, richer scheduling, data export, accessibility review, and real-device validation remain in progress.
>
> **What's coming:** TOVI is growing beyond the phone app. TOVI for Chrome, TOVI Kids Mode, and the community theme ecosystem are in development/planning and are **not available yet**. They are being introduced here so people can follow the direction of the project—not as features that can be downloaded or used today.

## The TOVI approach

- **Your time, your rules** — choose a daily allowance that fits your life.
- **One minute at a time** — the token model makes selected-app usage understandable.
- **Helpful, not noisy** — reminders are designed around meaningful milestones, not constant interruptions.
- **No shame, no lectures** — TOVI is about intentional choices, not guilt.
- **Your setup, your style** — deeper customisation is part of the long-term vision, with room for community-made themes.

## Product experience

The Android app is being built around four core areas:

1. **Home** — see your daily allowance and a clear view of the day.
2. **Apps** — choose which apps count toward your allowance.
3. **Insights** — understand patterns over time.
4. **Settings** — set your allowance, reset time, and visual preferences.

### Token model

The intended model is **1 token = 1 minute** of foreground use in an app selected by the user. Background time should not consume tokens. The daily allowance and reset schedule will be configurable. When the allowance is exhausted, the current development experience pauses access to selected apps until the next reset.

### Milestone reminders

The planned reminder points are 25%, 50%, 75%, 90%, and 100% of the daily allowance. These are intended to be meaningful check-ins rather than recurring nags.

## Beyond Android: what's on the way

These are future parts of the TOVI ecosystem. **They are not released, and there are no public downloads for them yet.** Their designs and capabilities may change during development.

### 🌐 TOVI for Chrome — a calmer corner of the browser

A browser should help you get somewhere—not pull you into an endless loop. TOVI for Chrome is planned as a browser companion with a customisable New Tab experience and more intentional browsing tools.

The direction includes:

- **A personal New Tab:** a thoughtful home for your day, with a clock, search, useful shortcuts, and optional widgets.
- **Make it yours:** adjustable layouts, colours, typography, backgrounds, and widget visibility.
- **Website time awareness:** a planned view of time spent on selected websites, with clear controls over what is counted.
- **Gentle nudges:** optional reminders to pause or leave a distracting site when you've reached a limit.

The extension is being developed as a separate browser experience. Website tracking, interventions, and site limits are not being presented as available features until they are implemented and tested. It will not claim to monitor your Android apps; that remains the job of the Android app and its separately granted permissions.

### 🎨 Community Themes — make TOVI feel like yours

TOVI should not have to look the same on everyone's device. The longer-term plan is to let people customise the experience deeply—and let the community create styles for one another.

The planned theme ecosystem may include:

- Community-created themes for different moods, colour palettes, and visual styles.
- Theme previews before applying them.
- Import and export so people can back up or share their own creations.
- A community gallery where creators can publish themes and others can discover them.
- Clear controls to return to the official TOVI look at any time.

The goal is customisation with freedom: thoughtful defaults for people who want simplicity, and room to experiment for people who want to tune every detail. Community publishing, sharing, and theme importing are **not available yet**.

### 🧸 TOVI Kids Mode — big adventures, little screen-time habits

A grown-up's digital wellbeing app shouldn't just be shrunk down and handed to a child. TOVI Kids Mode is planned as a brighter, friendlier experience—with its own personality, playful visuals, and simple language.

Think of it as a cheerful little space where children can understand their screen-time boundaries without feeling told off:

- **A colourful home:** friendly illustrations, warm colours, and clear cards that make the day easy to understand.
- **Little time missions:** simple, age-appropriate explanations of how much screen time is available.
- **Friendly pause moments:** gentle prompts to take a break, stretch, play, read, or do something away from the screen.
- **A grown-up corner:** parent-managed settings for schedules, app/site boundaries, and approvals.
- **Clear explanations:** children should be able to understand why a limit appears and what happens next.

Kids Mode is intended to feel distinct from the regular TOVI experience—more playful and welcoming, while still respecting children's privacy and dignity. Parent controls, family linking, child profiles, and cross-device management need careful design and testing.

**TOVI Kids Mode is a planned feature, not a released app or active parental-control service.** No parent or child account can be created for it yet.

## Current Android implementation

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
| TOVI for Chrome | In development / planned; not released |
| Community Themes and theme sharing | Planned; not released |
| TOVI Kids Mode and family controls | Planned; not released |

TOVI asks the user to enable its Accessibility service manually in Android Settings. The service detects foreground changes for selected apps, records foreground seconds locally, and converts each accumulated minute into one token. The pause overlay appears when a selected app opens. A configurable 3-, 5-, or 10-second pause precedes a five-minute continuation while tokens remain; the intervention includes a gentle breathing animation, and time on the pause screen itself is not counted. When the allowance reaches zero, the selected app is paused with no continue option until the next local-day reset. Home and Insights read the local usage ledger. The service does not inspect screen text, messages, photos, or keystrokes. Android OEM background restrictions and overlay behaviour still require real-device validation.

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

1. Finish reliable foreground-app usage tracking and real-device validation.
2. Complete the Android token experience, milestone notifications, and transparent user-authorised interventions.
3. Complete privacy, accessibility, and distribution-policy reviews; prepare the first signed Android release.
4. Develop the TOVI for Chrome experience, starting with the custom New Tab and then carefully validated website-time features.
5. Design the community theme format, creator workflow, import/export, and discovery experience.
6. Develop TOVI Kids Mode, including age-appropriate UX, parent-managed settings, and a secure family model.
7. Evaluate the iOS approach using Apple's supported Screen Time APIs.

Roadmap items are goals, not release promises. Features will be marked available here only when they have actually shipped.

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
