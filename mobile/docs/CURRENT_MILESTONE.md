# Current Milestone – MVP A / Phase 1

- **Date:** 2026-02-24
- **Scope:** BLE discovery (scan + advertise + UI counters)
- **Status:** Implementation complete, awaiting Josko-run validation on Samsung S23 + A17.
- **Artifacts:**
  - `data-ble/PresenceDiscoveryController.kt` – unified advertiser/scanner emitting Flow metrics.
  - `domain/discovery/PeerDiscoveryModels.kt` – canonical peer event + metrics models.
  - `app/ui` updates – runtime permission flow + live counters.
  - `docs/QA_STATUS.md` – tracking required human validation results.

Next milestone (pending approval): Phase 2 – BLE GATT transport + handshake framing.
