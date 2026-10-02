# LifeOps

LifeOps is a local-first Android command center for tasks, follow-ups, projects,
notes, expenses, documents, reminders, and everyday tools. Its central idea is
to keep the next action visible for everything that is active or waiting.

## Product overview

- Deep-black and silver liquid-style Compose UI with edge-to-edge layout.
- Home dashboard for attention items, today, waiting items, projects, spending,
  and fast capture.
- Task, follow-up, project, note, expense, and document records with local
  persistence, search, archive/restore, and detail editing.
- Follow-up waiting counters, timelines, next-action fields, and reminder
  scheduling.
- Percentage, EMI, date-difference, age, electricity, fuel, and expense tools.
- Calendar, settings, ZIP backup/restore, an Android widget, and an assistant
  abstraction ready for future offline/remote intelligence.

## Architecture

The app uses Kotlin, Jetpack Compose, Material 3, Room, Kotlin Coroutines and
Flow, ViewModel, Navigation Compose, WorkManager, DataStore, and KSP. Code is
organized by `core` infrastructure/design and feature-oriented UI/data files.
The database is local-only and the entities are designed so sync or an AI
provider can be added without making the first version dependent on a server.

## Build instructions

Requirements: JDK 17, Android SDK 36, and an Android 10 (API 29) or newer
device/emulator.

```bash
./gradlew testDebugUnitTest assembleDebug assembleDebugAndroidTest
./gradlew connectedDebugAndroidTest  # disposable API 29+ emulator/device
```

The debug APK is written to
`app/build/outputs/apk/debug/app-debug.apk`. The GitHub Actions workflow runs
the unit tests, assembles the APK, verifies its metadata, and publishes the APK
and a clean `LifeOps-Source.zip` artifact. It also runs isolated Compose/Room
instrumentation on API 29 and API 36 for pushes and pull requests to `main`.
See [TESTING.md](TESTING.md) for coverage, reports, and remaining manual QA.
Instrumentation clears app data; use a disposable test installation.

## Major features

Home dashboard, universal capture, tasks with status/priority/due dates and
recurrence, follow-ups with waiting duration and history, project next actions
and progress, quick notes, grouped search, reminders, settings, expenses,
documents via the system picker, calculators, calendar, archive, backup/restore,
and the LifeOps widget.

## Known limitations

- This is a debug build; release signing, Play Console configuration, and
  production crash reporting are not included.
- Data remains on-device. Cloud sync, multi-device backup, and AI execution are
  intentionally future work.
- Notification behavior still depends on Android notification permission and
  device power-management settings.
- Automated UI/device coverage is not yet a substitute for a full manual pass
  on every Android OEM and form factor.

## Future roadmap

Release signing and production QA, richer calendar interactions, widgets with
more actions, safer import conflict resolution, optional encrypted cloud backup,
and a confirm-before-change AI assistant for structured LifeOps data.
