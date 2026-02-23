# Presence Android App

The Android client (“Presence”) is the primary surface for generating Presence Proof bundles and earning PRESENCE tokens. Initial implementation targets a single-device developer workflow in Android Studio (macOS/Windows support comes later; for now we develop on Linux/mac via Android Studio).

## Modules (planned)

| Module | Purpose |
|--------|---------|
| `app` | Jetpack Compose UI, onboarding, debug screens. |
| `core:sensors` | GPS/WiFi/barometer sampling, bucket hashing, battery budgeting. |
| `core:ble` | Witness discovery, handshake, token exchange, retry policy. |
| `core:crypto` | Ed25519 key rotation, CBOR receipt signing (using BouncyCastle). |
| `data:queue` | SQLite/SQLDelight offline queue with WorkManager sync tasks. |
| `feature:proofs` | Epoch scheduler, proof builder, metrics instrumentation. |

## Immediate Goals (P0 → P1)

1. Scaffold Gradle project in Android Studio Iguana (AGP 8.3+, Kotlin 1.9).
2. Implement BLE handshake prototype using mock witnesses to validate API surface.
3. Wire sensor sampling + bucket hashing with <5%/epoch battery budget instrumentation.
4. Build CBOR receipt generator aligned with `architecture/RECEIPT_FORMAT.md` and export test vectors.
5. Integrate WorkManager sync job that POSTs receipts to the host API stub.

## Developer Notes

- Minimum SDK 26, target SDK 34.
- Compose Navigation for flows, Material3 UI.
- Use `kotlinx-serialization` + `com.chellow.cbor` (or similar) for on-device CBOR encoding until a custom encoder is required.
- All BLE + sensor operations must respect Android background limits: use foreground service for live witness hunts and schedule 10‑minute epochs via `WorkManager`.
- Logging via `Timber` with hooks into the test harness (host PC collects metrics via adb logcat).

A detailed build guide will live in `docs/SETUP.md` once the modules exist. For now, this README is the contract for the Android Studio project Ian requested.
