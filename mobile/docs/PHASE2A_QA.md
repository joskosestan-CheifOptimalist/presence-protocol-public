# Phase 2A QA Script — BLE GATT Transport Prep

> **Scope:** Establish a repeatable QA routine ahead of Phase 2A development. Actual GATT logic is not implemented yet; this script validates Phase 1 regression and defines how Phase 2A will be verified once code lands.

## A. Preconditions Checklist
1. **Hardware**
   - Samsung Galaxy S23 (example device ID: `R5CR700RAQF`)
   - Samsung Galaxy A17 (example device ID: `R5GYC0FZ6RY`)
   - Only one device may be connected to the workstation via USB at any time.
2. **Device settings**
   - Developer Options enabled (tap Build Number 7×).
   - USB debugging turned **ON**.
   - Stay Awake (optional) to avoid screen off during test.
   - Battery optimization disabled for the Presence app (Settings → Battery → Unrestricted).
3. **Bluetooth + Location**
   - Bluetooth toggled ON before launching the app.
   - Location services ON (required for BLE scanning on Android 12−).
4. **Permissions**
   - On first launch, grant requested permissions (Bluetooth Scan/Connect/Advertise on Android 12+, Fine Location on Android ≤11).
5. **Workspace**
   - Repo root: `/home/josko/.openclaw/workspace/presence-protocol-repo`
   - JAVA_HOME set to Corretto 17: `/home/josko/.openclaw/workspace/amazon-corretto-17.0.18.9.1-linux-x64`

## B. Build & Install Steps
1. Ensure only ONE device is connected via USB (`adb devices`). Disconnect the other.
2. Build the APK from repo root:
   ```bash
   cd /home/josko/.openclaw/workspace/presence-protocol-repo/mobile
   JAVA_HOME=/home/josko/.openclaw/workspace/amazon-corretto-17.0.18.9.1-linux-x64 ./gradlew assembleDebug
   ```
3. Output APK path: `app/build/outputs/apk/debug/app-debug.apk`.

## C. ADB Command Reference
> Repeat the sequence separately for S23 and A17 (one connected at a time).

```bash
# 1. Verify device connection
adb devices

# 2. Uninstall previous build (ignore errors if not installed)
adb uninstall com.presenceprotocol.app

# 3. Install latest debug APK
adb install app/build/outputs/apk/debug/app-debug.apk

# 4. Launch the app
adb shell monkey -p com.presenceprotocol.app -c android.intent.category.LAUNCHER 1
# or
adb shell am start -n com.presenceprotocol.app/.ui.MainActivity

# 5. Clear existing logs
adb logcat -c

# 6a. Capture logs filtered by tag
a db logcat PresenceDiscovery:D PresenceApp:D *:S \
  > docs/qa_logs/phase2a_<device>.log

# 6b. Alternate: capture using PID filtering (requires PID lookup)
PID=$(adb shell pidof com.presenceprotocol.app)
adb logcat --pid=$PID \
  > docs/qa_logs/phase2a_<device>_pid.log
```
Replace `<device>` with `s23` or `a17` to keep files distinct. After capture, stop logging with `Ctrl+C` once the test run completes.

## D. Phase 2A Readiness Pass/Fail (Regression Gate)
Even without Phase 2A code, the following must hold:
- **Discovery lifecycle**: Logs show `PresenceDiscovery started/stopped` entries when app enters/leaves foreground.
- **Permissions**: First launch prompts for Bluetooth permissions exactly once; subsequent launches should proceed without prompts (unless revoked).
- **Stability**: No crashes or ANRs when the app is foregrounded, backgrounded, or killed.
- **Counts**: `Peers Nearby` counter increments when a second device is running the app nearby (Phase 1 behavior), proving regression-free operation.
Failure of any item blocks Phase 2A implementation.

## E. When Phase 2A Code Exists (Future QA)
Once GATT transport is implemented, extend the test as follows:
1. **Service Discovery**
   - Confirm log entries: `GattServer initialized`, `Service 7d3a2d6b-...c01 added`, `Characteristics ...c02/...c03/...c04 ready`.
   - On the client device, capture log entries confirming `discoverServices` success and presence of all UUIDs.
2. **Characteristic Operations**
   - Verify `HELLO` write success logs on client and receipt logs on server.
   - Confirm `REPLY` notifications received and logged by client.
   - Confirm `RESULT` write success and final handshake complete log on server.
3. **Timing Validation**
   - Measure timestamps between connect and final RESULT log; must be ≤20 s.
4. **Cooldown/Retry Hooks**
   - After a successful handshake, immediately attempt another and confirm logs show `cooldown active` and handshake is skipped.
5. **Artifacts**
   - Save client/server log pairs per device to `docs/qa_logs/phase2a_<device>_handshake.log`.
   - Document PASS/FAIL details in `docs/QA_STATUS.md`.

After both devices finish the procedure, commit the new log files and QA notes (once Phase 2A code exists). Until then, this script ensures readiness without touching implementation.
