# Limity

**Make time count.**

Limity is a mindful social-media usage and digital-wellbeing app. It helps people make intentional choices about screen time through a configurable daily token allowance, thoughtful milestone reminders, and app blocking when their allowance is used.

> Android first. iOS is planned next.

## Product principles

- **Your allowance, your rules:** set your own daily token budget.
- **Usage-based tokens:** one token represents one minute in a selected app.
- **Mindful, not nagging:** reminders are tied to usage milestones rather than constant interruptions.
- **Personal expression:** choose an accent colour for the app.
- **No judgement:** Limity encourages intentional choices, not guilt.

## Planned features

- [x] Initial Android project and Compose interface
- [x] Custom daily token allowance setting
- [x] Accent colour selection
- [x] Android usage-access settings entry point
- [x] GitHub Actions debug APK workflow
- [ ] Select monitored apps
- [ ] Accurate foreground app usage tracking
- [ ] Daily token accounting and reset schedule
- [ ] Configurable milestone reminders
- [ ] Blocking screen when allowance is exhausted
- [ ] Usage insights and history
- [ ] Privacy and accessibility review
- [ ] iOS version using Apple's supported Screen Time APIs

## Token model

The intended model is **1 token per minute** of foreground use in apps selected by the user. Only selected apps count. Background time should not consume tokens. The daily allowance and reset schedule will be configurable.

## Accent colours

The initial interface includes Sage, Blue, Purple, Orange, Rose, and Teal. The selected accent is saved locally on the device.

## Android development

- Language: Kotlin
- UI: Jetpack Compose and Material 3
- Minimum Android version: Android 8.0 (API 26)
- Compile/target SDK: Android 35
- JDK: 17

Open this repository in Android Studio and allow Gradle to sync. A debug APK can also be built with:

```bash
gradle assembleDebug
```

GitHub Actions builds the debug APK on pushes to `main` and manual workflow runs. Download the generated artifact from the workflow run's **Artifacts** section.

## Current development status

This is an early development build. The current interface lets you configure and locally save the daily token allowance and accent colour. **Live usage tracking, token deduction, milestone notifications, app selection, and enforcement/blocking are not implemented yet.** The usage-access button currently opens Android's system settings; it does not mean monitoring is already active.

Limity will request only the permissions needed for its wellbeing features and explain why they are needed. Android app-usage access and blocking have platform and distribution-policy constraints that must be reviewed before release.

## Roadmap

1. Finish Android usage monitoring and the token engine.
2. Add app selection, milestones, resets, and blocking.
3. Test on real Android devices and improve accessibility.
4. Prepare a release build and privacy documentation.
5. Begin the iOS implementation.

---

Made to help you spend your time on what matters.


## TOVI brand and website

The product brand is now **TOVI** — “Time, on your terms.” The official website source lives in this repository:

- `index.html` — responsive TOVI website
- `assets/css/style.css` — visual system (Manrope + DM Sans; 60/30/10 palette)
- `assets/js/main.js` — live GitHub Releases download feed
- `assets/tovi-mark.svg` — geometric brand mark and favicon
- `.github/workflows/pages.yml` — automatic GitHub Pages deployment

The download centre reads the public GitHub Releases API for this repository, lists the latest stable release and recent release history, and refreshes every five minutes. It will show an honest “no release yet” state until an official release with an APK asset is published.

### Enable the website

In the repository, open **Settings → Pages**, set **Build and deployment → Source** to **GitHub Actions**, and save. The workflow will then deploy the site on website-related pushes to `main`.

Expected site URL: https://iy367-ux.github.io/Limity/

Note: the Android application is still in early development. The website describes the planned token and wellbeing experience but does not claim that unfinished monitoring or blocking features are already available.
