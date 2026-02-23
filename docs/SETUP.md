# Workspace Setup

## 1. Tooling Prerequisites

| Area | Requirement | Notes |
|------|-------------|-------|
| Android | Android Studio Iguana (2023.2.1) + JDK 17 | Enable `Android SDK 34`, install NDK r26 for BLE debugging. |
| Kotlin | Kotlin 1.9.x, Gradle 8.5 (wrapper) | Use JetBrains Toolbox or SDKMAN. |
| Node | Node 20.x + pnpm 9.x | For Lucid scripts + SDK tooling. |
| Rust | Rust 1.76+, `rustup` | Needed for host services if we choose Axum. |
| Cardano | `cardano-node` + `cardano-cli` 8.x, `ogmios` (optional), Blockfrost key | Export `CARDANO_NODE_SOCKET_PATH` when running local node. |
| Midnight | Devnet SDK (pending) | Until GA, use MockMidnight service in `backend/mock-midnight/`. |
| Databases | Docker Desktop or Podman | Runs TimescaleDB + Redis locally. |

## 2. Repo Bootstrap

```bash
# From repo root
cp .env.example .env                # populate API keys, Cardano creds
sdkmanager "platform-tools"        # ensure adb available
./gradlew tasks                     # fetch Android deps
pnpm install --dir onchain/lucid    # install Lucid deps
cargo check --manifest-path backend/Cargo.toml  # if using Rust host
```

## 3. Local Services

```bash
# Timescale + Redis
make services-up
# or
cd infra && docker compose up -d timescaledb redis
```

## 4. Credentials to request

- Cardano Preprod faucet wallet + signing keys.
- Blockfrost (or equivalent) API key for Preprod.
- Midnight devnet credentials (once available).
- Firebase (for push notifications) if needed for Android beta.

## 5. Recommended IDE Extensions

- Android Studio: Kotlin, Compose, BLE Scanner, ktlint.
- VS Code / IntelliJ: Markdown All-in-One, Mermaid Preview for architecture diagrams.
- GitHub Copilot / Claude Dev plugin (optional) for codegen.

> Keep this file updated as we formalize CI/CD and environment automation.
