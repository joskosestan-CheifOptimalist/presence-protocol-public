# Architecture Overview

This document captures the MVP component layout for Presence Protocol. It will evolve as we move through phases P0–P2.

## Component map (MVP)

| Layer | Responsibility | Primary Stack |
|-------|----------------|---------------|
| Android Presence App | Collect GPS/WiFi/barometer samples, run BLE witness exchange, build CBOR receipt, queue offline bundles | Kotlin + Jetpack Compose + Android BLE APIs |
| Host Services | Receipt ingestion API, queue, risk-scoring, Merkle batcher, monitoring/dashboard | Kotlin + Ktor (or Rust Axum) + Postgres/Timescale |
| On-chain Anchors | Publish batch roots + staking attestations on Cardano Preprod, optional Midnight privacy proofs | Plutus / Lucid scripts + Cardano CLI |
| Analytics + Supervision | Metrics dashboard, fee calculators, beta cohort monitoring | Grafana / Metabase fed from Postgres + Timescale |
| SDK | Typed helpers for partners who want to verify receipts or integrate with the API | Kotlin Multiplatform + TypeScript clients |

## Threat model snapshot

1. **Sybil / fake presence**
   - Mitigations: BLE witness signatures + rotating device key pairs, minimum witness count per epoch, hash-based proof commitments.
2. **Replay / tampering**
   - CBOR receipts include epoch ID, nonce, and device signature; backend rejects duplicates and enforces idempotency keys.
3. **Hotspot spoofing / GPS drift**
   - Sensor fusion (GPS + WiFi + barometer) with coarse bucket IDs; backend cross-checks bucket transitions and witness overlap.
4. **Backend compromise**
   - Receipts are hashed and Merkle-rooted before anchoring; on-chain batch commitment allows external validators to re-verify.
5. **Data privacy**
   - Only 5km bucket IDs + hashes leave the device. Raw sensor samples never persist server-side.

Detailed threat tables + STRIDE mapping will be added during P1 once we finalize BLE and staking implementations.

## Dependency assumptions

- **Android tooling**: Android Studio Iguana, JDK 17, Kotlin Coroutines, Jetpack Compose UI, WorkManager for background tasks, SQLDelight for local queue.
- **Backend**: Kotlin + Ktor (or Rust Axum) with PostgreSQL + TimescaleDB. Queueing via Redis or embedded PostgreSQL advisory locks. gRPC/REST via OpenAPI spec generated during P1.
- **Cardano toolchain**: Cardano CLI 8.x, Ogmios or Blockfrost for Preprod integration, Lucid JS scripts for quick prototyping, Aiken for long-term Plutus validators.
- **Midnight**: Devnet SDK once available; until then, MockMidnight service per LEAD brief.
- **CI/CD**: GitHub Actions for lint/test on every push; mobile lanes use Gradle Managed Devices; backend uses JUnit + ktlint.

## Pending work items

- Flesh out BLE witness handshake sequence diagram + state machine.
- Add Merkle batcher specification (tree fan-out, cadence, failure recovery).
- Document staking / reward distribution contract interfaces once on-chain prototype exists.
