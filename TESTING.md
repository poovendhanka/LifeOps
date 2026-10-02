# Automated workflow coverage

## Run locally

Use JDK 17, Android SDK/platform 36, and an API 29+ emulator or disposable test
device. These instrumentation tests **clear the app's data**, so do not run them
against a device installation containing personal records.

```bash
./gradlew testDebugUnitTest assembleDebug assembleDebugAndroidTest
./gradlew connectedDebugAndroidTest
```

The workflow runs on pushes and pull requests to `main`, and manual dispatch.
It builds the application and test APK, runs the existing eight JVM rule tests,
and runs instrumentation on API 29 (minimum supported) and API 36 (target).
Android Test Orchestrator clears package data and restarts instrumentation for
every case. There are no timing sleeps or mocked navigation, ViewModel, Room,
JSON, or backup implementations. Compose synchronization and bounded waits for
Room writes are used instead.

## Coverage

| Area | Assertions |
| --- | --- |
| Tasks | Blank-title rejection; trimmed creation; edit title, next action, status and priority; stable ID/creation time; single record; status history; saved data visible after activity relaunch |
| Editor lifecycle | Unsaved task survives activity recreation; discard writes nothing |
| Follow-ups | Waiting record/contact; timeline updates and last action; resolve status/history; active and resolved filters |
| Universal capture | Task, follow-up, note, project and expense creation; titleless Tamil note; invalid/valid money; missing document attachment rejection; capture intent consumed across recreation |
| Search | Case-insensitive, trimmed query across all six kinds and multiple metadata fields; group counts; archived/unrelated exclusion; detail navigation and retained query; empty/no-result states |
| Archive/restore | Confirmation cancellation; removal from active Room flow; Settings archive and restore; same ID, fields and timeline retained |
| Backup/restore | Public ZIP API round trip of all entity fields, linked records, archived records, Unicode, history and reminder reconciliation; inaccessible URI stripped; newer-only merge; equal/older records retained; local-only records preserved; idempotent repeated import |
| Invalid backup | Orphan timeline transaction rollback; duplicate IDs; invalid fields; unsupported format/version; malformed JSON; wrong ZIP entry; decompressed 20 MB limit; existing data preserved |
| Room views | Pinned/recent ordering and active/archive query results before and after restore |

`app/src/androidTest/java/com/lifeops/WorkspaceTest.kt` resets the real on-disk
workspace. `CoreWorkflowsTest.kt` launches the actual `MainActivity` and navigates
its Compose screens. `BackupRestoreTest.kt` uses cache-file URIs through the real
ContentResolver and database transaction; it does not automate a vendor-specific
Storage Access Framework picker. Small semantic test tags identify scroll
containers and controls without changing their visual layout.

## Reports and remaining QA

GitHub Actions uploads `lifeops-debug-build` with JVM reports and build artifacts,
and `lifeops-device-tests-api-29` / `lifeops-device-tests-api-36` with instrumented
HTML/XML reports and device test output, including on failure. Local reports:

- `app/build/reports/tests/testDebugUnitTest/index.html`
- `app/build/reports/androidTests/connected/debug/index.html`

This is focused regression coverage, not production certification. Physical OEM
devices, process death, accessibility/font scaling, screen sizes, system document
picker grants and attachment success, notification delivery under battery limits,
and release signing still need dedicated QA. Backup tests check reconstruction of
reminder records, not actual notification delivery. Settings and attachment file
bytes are not included in the current backup format.

Configuration references:
[Android Test Orchestrator](https://developer.android.com/training/testing/instrumented-tests/androidx-test-libraries/runner#use-android)
and [Android Emulator Runner](https://github.com/ReactiveCircus/android-emulator-runner).
