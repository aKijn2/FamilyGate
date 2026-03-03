# FamilyGate

FamilyGate is a parental-control Android app backed by a cloud [PocketBase](https://pocketbase.io/) database. A parent configures rules (block apps, set daily time limits) from the PocketBase admin panel, and those rules are automatically enforced on the child's device.

---

## How it works — overview

```
Parent (PocketBase admin panel)
   └─► Creates/edits rules in app_rules collection
           │
           │  (HTTPS, every 15 min automatically)
           ▼
Child device (FamilyGate APK)
   └─► SyncWorker downloads rules → saved locally
   └─► AccessibilityService watches every app that opens
           │
           ├─ App is blocked? → Show "App Blocked" screen
           └─ App is allowed? → Do nothing
```

There is **one APK** shared by both parent and child. The difference is just which device it is installed on and what `childDeviceId` is configured.

---

## Architecture

### PocketBase (cloud backend)
- Hosts the `users` collection for parent login.
- Hosts the `app_rules` collection where the parent stores per-app rules for each child device.
- Runs in the cloud (e.g. Northflank), so rule changes propagate to all child devices automatically.

### Android app components

| Component | What it does |
|---|---|
| `MainActivity` | Configuration UI: PocketBase URL, parent email/password, child device ID, manual sync button, app list |
| `MainViewModel` | Business logic for the UI: orchestrates sign-in → fetch rules → save rules |
| `PocketBaseClient` | HTTP client: authenticates with PocketBase and fetches `app_rules` records |
| `LocalRuleStore` | Saves all rules and credentials to `SharedPreferences` on the device |
| `SyncWorker` | Background WorkManager job — runs every 15 minutes to pull new rules from PocketBase |
| `ParentalAccessibilityService` | Android Accessibility Service — watches every foreground app change |
| `ParentalControlManager` | Decides whether a given package should be blocked based on local rules |
| `BlockedAppActivity` | Full-screen "This app is blocked" screen shown when a blocked app is opened |

---

## Step-by-step setup

### 1. PocketBase — one-time setup

1. Go to your PocketBase admin panel (e.g. `https://your-instance.code.run/_/`)
2. Create a collection named `app_rules` with these fields:

   | Field | Type | Required |
   |---|---|---|
   | `childDeviceId` | Text | ✓ |
   | `packageName` | Text | ✓ |
   | `appName` | Text | |
   | `blocked` | Bool | |
   | `dailyLimitMinutes` | Number (integer) | |

3. In **Collections → users**, create a new user record with the email and password you will enter in the app.

### 2. Child device — install & configure

1. Install the APK on the child's device.
2. Open FamilyGate and fill in:
   - **PocketBase URL** — e.g. `https://family--pocketbase--qb9jcrrdp86n.code.run`
   - **Parent email** — the email of the user you created in step 1
   - **Parent password** — same
   - **Child device ID** — a unique label for this device, e.g. `child-device-1`
3. Tap **Save config**.
4. Tap **Open Usage Access Settings** → find FamilyGate → enable it.
5. Tap **Open Accessibility Settings** → find FamilyGate → enable it.
6. Tap **Sync rules** once manually to do the first sync.

From this point, rules sync automatically every 15 minutes in the background.

### 3. Parent — add and manage rules

In the PocketBase admin panel, create records in `app_rules`:

| childDeviceId | packageName | appName | blocked | dailyLimitMinutes |
|---|---|---|---|---|
| child-device-1 | com.instagram.android | Instagram | true | 0 |
| child-device-1 | com.google.android.youtube | YouTube | false | 30 |

The child device will pick them up within 15 minutes automatically.

---

## Can the child remove permissions themselves?

**Short answer: yes, in this MVP they can.** Here is what a child can do and how it affects the app:

### Disabling the Accessibility Service
- The child can go to **Settings → Accessibility → FamilyGate** and turn it off.
- **Effect:** The foreground app watcher stops. Blocked apps can be opened freely.
- **Detection:** The main screen shows "Status: disabled" for the Accessibility permission when the app is opened next time, alerting the parent if they check.
- **Prevention in this MVP:** None — Android does not allow any app to lock its own accessibility permission without Device Owner/Admin privileges.

### Disabling Usage Access
- The child can go to **Settings → Apps → Special app access → Usage access** and disable FamilyGate.
- **Effect:** The app usage dashboard stops showing minutes used. Time-limit enforcement stops working. Blocking by `blocked = true` still works as long as Accessibility is still on.
- **Prevention in this MVP:** None for the same reason as above.

### Force-stopping the app
- The child can go to **Settings → Apps → FamilyGate → Force stop**.
- **Effect:** The `SyncWorker` background job is cancelled temporarily (Android will restart it later). The Accessibility Service also stops.
- **Note:** WorkManager re-schedules itself the next time the app is opened.

### Uninstalling the app
- On standard Android a child can uninstall any app through Settings → Apps.
- **Effect:** All protection disappears.
- **Prevention in this MVP:** None without Device Admin mode.

---

## Why these limitations exist and how to fix them

This app is built as a standard Android app. Standard apps have no authority to prevent a user from disabling their permissions or uninstalling them. Full parental control requires one of these approaches:

| Approach | Protection level | Complexity |
|---|---|---|
| **This MVP (standard app)** | Low — child can bypass by disabling permissions | Low |
| **Device Administrator** (`DevicePolicyManager`) | Medium — can prevent uninstall | Medium |
| **Device Owner** (MDM, provisioned at setup) | High — can lock down entire device | High |
| **Managed Profile** (work profile) | High — full app sandbox | High |

For a school or family deployment where the parent controls device setup, **Device Owner** or **Android for Work managed profiles** are the correct long-term solutions.

---

## Automatic background sync

After tapping **Save config**, WorkManager registers a periodic background task (`SyncWorker`) that:
1. Reads the saved PocketBase URL, email, password, and child device ID from local storage.
2. Authenticates with PocketBase.
3. Downloads all `app_rules` records matching the child device ID.
4. Saves them locally so the Accessibility Service can read them instantly without network access.

The minimum interval Android allows for WorkManager periodic tasks is **15 minutes**. The actual run time may be delayed slightly by Android's battery optimisations (Doze mode), but will always eventually run.

The task survives:
- App being closed
- Device reboot
- App update

It does **not** survive:
- Force-stop (until the app is opened again)
- Uninstall

---

## Security notes

- **Credentials are stored in plain text** in `SharedPreferences`. For production, use Android Keystore + `EncryptedSharedPreferences`.
- **No SSL pinning** — the app trusts any valid HTTPS certificate. A determined attacker on the same network could intercept traffic.
- **PocketBase API rules** — make sure the `app_rules` collection in PocketBase has API rules set so that only authenticated users can read records. Otherwise anyone with the URL can read your rules.
- **Child device ID is not verified** — any device that knows the parent credentials and a childDeviceId can read those rules. There is no binding between a physical device and an ID.

  - Parent/child split apps with secure token flow
