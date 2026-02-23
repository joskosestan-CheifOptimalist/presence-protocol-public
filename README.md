# Presence Protocol

Privacy-preserving proof-of-presence + witnessing primitive for the Cardano ecosystem.

## Repository layout
- `docs/` – whitepaper + planning PDFs, setup guide, weekly status template.
- `architecture/` – protocol + system design (BLE handshake, CBOR receipts, threat model).
- `roadmap/` – six-month solo MVP milestones.
- `mobile/` – Android Presence app (Jetpack Compose, BLE, sensors).
- `backend/` – ingestion, batching, API (stubbed).
- `onchain/` – Cardano anchoring scripts (stubbed).
- `sdk/` – verifier SDK + tools (stubbed).

## Getting Started
1. Read `docs/SETUP.md` for toolchain requirements (Android Studio, Cardano CLI, etc.).
2. Browse `architecture/` for component + threat-model overview.
3. Track weekly progress via `docs/STATUS_TEMPLATE.md` and `presence-protocol/ARCHITECT_LOG.md` (outside repo for now).

## Status (2026-02-20)
- Planning documents committed to `docs/`.
- Architecture doc expanded with component map + threat snapshot + dependency assumptions.
- Mobile module design documented; Android Studio project scaffolding kicks off during P0.
- Setup + status templates added to guide incoming contributors.
