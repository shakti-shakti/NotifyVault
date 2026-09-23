# NotifyVault

### A private, local-first notification vault for Android

NotifyVault captures the notifications that matter, keeps them searchable, and gives sensitive alerts a controlled place to live. It combines a local notification archive, persistent duplicate suppression, background OTP delivery, notification replay, and a security-first vault experience in a polished Jetpack Compose interface.

> **Status:** Android application, actively developed  
> **Minimum Android version:** Android 8.0 (API 26)  
> **Target SDK:** Android 16 / API 36  
> **Application ID:** `com.aistudio.notifyvault.ovltxd`

---

## Why NotifyVault

Important notifications are easy to lose:

- A verification code disappears beneath a busy notification shade.
- A delivery update is posted repeatedly and fills the archive with duplicates.
- A notification remains visible after its original app has forgotten the detail.
- A useful notification action or deep link is difficult to recover later.

NotifyVault turns those moments into a private, organized timeline without relying on a hosted backend. Capture, indexing, OTP handling, app icons, replay data, lock state, and preferences are designed to stay on the device.

---

## Core capabilities

### Notification vault

- Captures notifications through Android's `NotificationListenerService`.
- Stores rich notification content locally with Room.
- Preserves title, text, big text, summary, channel information, actions, people, group metadata, images, links, timestamps, and source-app identity where available.
- Supports search, filters, category views, starring, archiving, read state, and notification details.
- Shows real installed-app icons for notification sources and app-picker selections.
- Excludes NotifyVault's own notifications before they can enter the vault.

### Smart duplicate suppression

NotifyVault creates a stable fingerprint from the source package, notification identity, and visible content.

- Identical reposts are skipped instead of creating another row.
- Changed content updates the existing notification and records a history entry.
- Update counts and previous versions remain available for audit and review.
- Deduplication decisions are serialized on the background I/O path.
- “Show updates separately” is available when users prefer a full event stream.

### Always-on OTP catcher

When an incoming notification matches OTP-style content, NotifyVault can produce its own private alert immediately.

- Detects common 4–8 digit verification codes with contextual keywords.
- Prefers the most useful six-digit match and rejects common false positives.
- Displays optional emoji digits for quick visual recognition.
- Copies the raw code, not the emoji representation.
- Copy and Dismiss cancel the exact NotifyVault OTP notification.
- Supports custom sound, vibration, lock-screen visibility, timeout, app selection, and excluded-app behavior.
- Persists a hash-based delivery claim so reposts and listener restarts do not create duplicate OTP alerts.
- Keeps the OTP history masked; raw codes are not stored in the persisted OTP log.

### Notification replay

NotifyVault stores the best available route back to a captured notification:

1. Live notification content action.
2. Live notification action.
3. Stored deep link.
4. Source-app launch.
5. Source-app details as a final fallback.

Replay is intentionally best-effort. Android does not allow every notification's original `PendingIntent` to be persisted forever, so NotifyVault reports when exact replay is no longer available instead of pretending otherwise.

### Privacy and vault security

- Local-first storage with no application server required.
- Configurable PIN, password, or 3×3 pattern lock.
- Optional biometric unlock.
- Automatic lock boundaries for app backgrounding, screen-off, reboot, and other lifecycle transitions.
- Secure-recents option to prevent sensitive previews.
- Emergency security wipe and permanent panic purge flows.
- Masked OTP history and hash-based OTP delivery state.
- No raw OTP values written to the archive deduplication state.

### Premium glass interface

- Jetpack Compose UI with the Obsidian Glass visual system.
- Vault, Search, Insights, and Customization Studio top-level destinations.
- Detail views, exclusion rules, replay diagnostics, quick chips, and app pickers.
- Consistent system-back handling:
  - nested screens return to their previous screen;
  - sheets close before navigation changes;
  - top-level destinations return to Vault;
  - the activity exits only from the Vault root.
- The supplied NotifyVault brand icon is used unchanged with transparent edges and no generated background or border.

---

## How capture works

```text
Android notification
        │
        ▼
NotificationListenerService
        │
        ├── Ignore NotifyVault-owned notifications
        ├── Register live PendingIntents for replay
        ├── Resolve source app and icon
        ├── Extract deep links and notification metadata
        ├── Detect and deliver OTP independently
        └── Fingerprint, deduplicate, and archive on Dispatchers.IO
```

OTP delivery intentionally runs independently from archive writes. A busy database, an exclusion rule, or an existing archive row must not delay a user-facing OTP alert.

After a reboot, package replacement, or listener disconnect, NotifyVault requests Android to rebind the authorized listener. A heartbeat worker provides an additional recovery path without toggling the service component.

---

## Privacy model

NotifyVault is designed around a local-only trust boundary:

| Data | Storage behavior |
| --- | --- |
| Captured notification content | Local Room database |
| Notification history | Local Room database |
| Source-app metadata and cached icons | Local app storage |
| Deep links | Local Room database when extracted |
| Live `PendingIntent` replay handles | In-memory only |
| OTP history | Local preferences, masked values only |
| OTP delivery deduplication | Local preferences, source/code hash only |
| Raw OTP used for Copy | Transient intent data; not written to the OTP log |
| Lock credentials | Local encrypted and salted credential state |

NotifyVault does not claim that Android's notification access is private from the operating system. Android grants the listener access, and users should install the app only on devices they trust.

---

## First-run setup

1. Install and open NotifyVault.
2. Complete the vault lock setup.
3. Grant **Notification Access** to NotifyVault in Android Settings.
4. On Android 13 and later, grant **Notifications** permission.
5. If prompted, allow battery optimization exemption or set the app to **Unrestricted** battery usage.
6. On OEM Android builds, allow background activity and auto-start where those controls exist.
7. Choose quick-chip apps and configure OTP source preferences as needed.

### Required Android access

| Access | Why it is used |
| --- | --- |
| Notification Access | Read and archive notifications and detect OTP content |
| Notifications (`POST_NOTIFICATIONS`) | Show OTP and service-status alerts on Android 13+ |
| Foreground service | Keep the active capture service visible to Android |
| Boot completed | Restore listener recovery after reboot |
| Wake lock | Keep short OTP delivery work alive while the device is waking |
| Battery optimization request | Reduce OEM/Doze interruption of capture |
| Biometric | Optional vault unlock |
| Vibrate | OTP copy confirmation and configured alert vibration |
| Installed-app queries | Populate source-app pickers and resolve source icons |

Android and device manufacturers can still stop or restrict background work when Notification Access is denied, notifications are blocked, the app is force-stopped, or OEM battery policies override the user's settings. NotifyVault uses every supported recovery path, but no application can bypass those system boundaries.

---

## Project structure

```text
.
├── app/
│   └── src/main/
│       ├── java/com/example/
│       │   ├── data/       # Room entities, repositories, replay, links
│       │   ├── security/   # Lock state, biometrics, lifecycle boundaries
│       │   ├── service/    # Listener, OTP, receivers, boot recovery
│       │   ├── ui/         # Compose screens, components, and theme
│       │   └── viewmodel/  # Vault state and user actions
│       └── res/            # Android resources and canonical app icon
├── scripts/
│   └── build-release.sh    # Toolchain setup, test, release, signature check
├── output/
│   └── NotifyVault-release.apk
├── BuildCommand            # Release command reference
└── README.md
```

---

## Technology

- Kotlin
- Jetpack Compose and Material 3
- AndroidX Lifecycle and Activity
- AndroidX Room
- AndroidX WorkManager
- AndroidX Security Crypto
- AndroidX Biometric
- Kotlin Coroutines and StateFlow
- Coil for image loading
- KSP for Room code generation
- Robolectric, Compose UI tests, and Roborazzi test support

---

## Build and test

The repository includes a self-contained release script that checks for Java 17+, prepares the Android SDK when needed, runs tests, assembles the signed release APK, syncs it to `output/`, and verifies the APK signature.

From the project root:

```bash
bash scripts/build-release.sh
```

The generated release artifact is:

```text
output/NotifyVault-release.apk
```

### Toolchain overrides

Use an existing SDK:

```bash
ANDROID_SDK_ROOT=/path/to/android-sdk bash scripts/build-release.sh
```

Use a custom local toolchain cache:

```bash
NOTIFYVAULT_TOOLCHAIN_DIR=/path/to/cache bash scripts/build-release.sh
```

Use a different release keystore:

```bash
KEYSTORE_PATH=/absolute/path/to/release-key.jks \
  bash scripts/build-release.sh
```

Release signing values should be supplied through environment variables or a secure CI secret store. Do not commit credentials, private keys, or generated signing files to source control.

---

## Operational notes

- The notification listener is an Android system-authorized service, not a conventional always-running process.
- Live replay handles are intentionally ephemeral and disappear after listener process death.
- Deep-link replay and app launch are graceful fallbacks, not guarantees that the original screen still exists.
- Notification source icons are preserved separately from the NotifyVault brand icon.
- OTP alert delivery depends on the source notification being visible to Android's notification listener.
- Exclusion rules can prevent archive storage while OTP capture remains available according to the configured OTP settings.
- Clearing the vault and emergency wipe actions are destructive and should be treated as irreversible.

---

## Release checklist

Before distributing a build:

- Confirm Notification Access and Android notification permission flows.
- Verify cold-start capture after a reboot.
- Verify OTP delivery with the app closed and the device idle.
- Verify duplicate reposts do not create duplicate archive rows or OTP alerts.
- Verify Copy cancels the exact OTP notification.
- Verify source-app icons appear correctly in picker and vault cards.
- Verify system back behavior from every nested screen and sheet.
- Run `bash scripts/build-release.sh`.
- Confirm the APK signature and inspect `output/NotifyVault-release.apk`.

---

## Design principles

1. **Capture once, preserve context.** Keep useful metadata with the notification instead of reducing it to a title and body.
2. **Separate delivery from archival.** A fast OTP alert must not wait on database work.
3. **Persist only what is necessary.** Mask sensitive history and hash delivery claims.
4. **Prefer honest fallbacks.** Report when exact replay is unavailable.
5. **Respect Android's boundaries.** Notification Access, permission grants, and OEM policies remain user-controlled.
6. **Keep the interface calm.** The vault should organize the notification shade without becoming another source of noise.
