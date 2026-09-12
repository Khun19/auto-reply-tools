# Auto Reply Tools

Native Android foundation for controlled Viber auto-replies through semantic
AccessibilityService interaction with ChatGPT or Gemini.

## Current scope

- Kotlin Android application with an explicit automation state machine.
- Viber `NotificationListenerService` intake.
- Sender whitelist and in-memory duplicate guard.
- Bounded polling and timeouts for every UI interaction.
- Semantic accessibility node matching and scoring; no coordinate taps.
- ChatGPT, Gemini, and Viber adapter boundaries.
- Emergency STOP and manual resume.

## Build gate

Run the following from the repository root when Android SDK and Gradle are
available:

```bash
gradle -p android :app:assembleDebug
```

An Android device/emulator is required for install/run verification. The
AccessibilityService and NotificationListenerService cannot be fully verified
in a JVM-only environment.