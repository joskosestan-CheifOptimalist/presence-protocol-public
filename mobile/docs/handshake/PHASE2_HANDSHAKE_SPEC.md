# Phase 2 Handshake Specification (Draft)

## 1. Roles & Advertising Strategy
- **Peripheral (GATT Server):** Hosts Presence service + characteristics, accepts incoming connections.
- **Central (GATT Client):** Initiates connection, performs service/characteristic discovery, writes HELLO and RESULT, subscribes to REPLY notifications.
- **Advertise/Scan:** Both devices advertise the Presence service UUID (`7d3a2d6b-9b7a-4f2a-9e5e-0c9d6f1b1c01`) and scan for it continuously (duty cycle inherited from Phase 1). Connection direction is decided deterministically per encounter (below).

## 2. Deterministic Connection Rule
- Compute `role_key = min(self_ble_address, peer_ble_address)` using lexicographic comparison of uppercase MAC addresses.
  - Device whose BLE address equals `role_key` becomes **Peripheral**; the other becomes **Central**.
  - If two devices choose the same role (rare edge), Central backs off (random 2–4s) and re-evaluates until handshake starts.

## 3. Packet Formats
Encoding: **CBOR** map with explicit version key `v` (uint). All payloads include a 16-byte Session ID (`sid`) and 32-byte Nonce (`nonce`). Byte lengths refer to raw binary values embedded in CBOR byte strings.

### 3.1 HELLO (client → server via write to HELLO characteristic `...c02`)
| Field | CBOR Key | Type | Length |
| --- | --- | --- | --- |
| Version | `v` | uint | 1 (current = 1) |
| Session ID | `sid` | bstr | 16 bytes |
| Nonce | `nonce` | bstr | 32 bytes |
| Client Public Key | `pk_c` | bstr | 32 bytes (Ed25519) |
| Timestamp | `ts` | uint | seconds since epoch |

### 3.2 REPLY (server → client via notify on REPLY characteristic `...c03`)
| Field | CBOR Key | Type | Length |
| --- | --- | --- | --- |
| Version | `v` | uint | 1 |
| Session ID | `sid` | bstr | 16 bytes (mirrors HELLO) |
| Nonce Echo | `nonce` | bstr | 32 bytes |
| Server Public Key | `pk_s` | bstr | 32 bytes |
| Signature | `sig_s` | bstr | 64 bytes (Ed25519 over canonical transcript) |
| Status Code | `code` | uint | 0 = OK, non-zero = error |

### 3.3 RESULT (client → server via write to RESULT characteristic `...c04`)
| Field | CBOR Key | Type | Length |
| --- | --- | --- | --- |
| Version | `v` | uint | 1 |
| Session ID | `sid` | bstr | 16 bytes |
| Signature | `sig_c` | bstr | 64 bytes (Ed25519 over transcript including REPLY) |
| Outcome | `code` | uint | 0 = Success, otherwise error |

**Canonical transcript (for signatures):** `CBOR(v, sid, nonce, pk_c, pk_s, ts)` concatenated per step. Implementation must ensure deterministic field ordering.

## 4. Crypto & Security
- **Key Type:** Ed25519 device keypairs stored securely on-device (software key + Keystore-wrapped secret per Phase 2 mandate).
- **Exchange:** Public keys included in HELLO/REPLY; no separate advertisement field.
- **Replay Protection:** Combination of 32-byte nonce + 16-byte session ID; server rejects HELLO if `nonce` or `sid` seen within last 10 minutes. Timestamps validated within ±60 s skew.

## 5. Timing & Duty Cycle
- **Connection timeout:** 8 s. If not connected within, Central aborts.
- **Service discovery timeout:** 5 s after connect; failure triggers retry once.
- **REPLY wait timeout:** 5 s after HELLO write.
- **Overall handshake deadline:** 20 s total.
- **Backoff:** On failure, Central waits random 5–10 s before new attempt with same peer.

## 6. Cooldown Policy
- **Success:** After RESULT code 0, both devices store peer ID and last success timestamp (in encounter storage). No new handshake attempts allowed for 10 minutes per peer pair.
- **Failure:** After ≥3 consecutive failures within 10 minutes, impose 10-minute cooldown. Single failure enforces 60 s retry block.
- **Storage:** Persisted via existing encounter storage module (Phase 3). For Phase 2A, keep in-memory map + flush to storage when available.

## 7. Logging Requirements
Each device MUST log:
- Role selection (`Peripheral`/`Central`) with BLE addresses.
- Connection lifecycle (`connect_start`, `connect_success`, `connect_timeout`).
- Service/characteristic discovery results and UUIDs.
- Write/notify actions with payload sizes (do not log raw keys/signatures).
- Signature verification outcome.
- Cooldown decisions (`cooldown_active`, `cooldown_reset`).
Logs use tag `PresenceHandshake` with structured key=value pairs for easy filtering.

## 8. Result Codes
| Code | Meaning |
| --- | --- |
| `0` | Success |
| `1` | TIMEOUT (connect/service/notify) |
| `2` | BUSY (peer already handshaking) |
| `3` | INVALID_SIG |
| `4` | REPLAY_DETECTED |
| `5` | PROTOCOL_ERROR (bad payload) |

## 9. Minimal Acceptance Criteria (Phase 2A)
- **Success rate:** ≥4 successful HELLO→REPLY→RESULT sequences out of 5 attempts between S23 and A17 within 20 s each.
- **Failure tolerance:** ≤1 timeout or protocol failure in a 15-minute QA session.
- **Artifacts:** Client + server logs saved to `docs/qa_logs/phase2_handshake_<device>.log` and noted in `docs/QA_STATUS.md`.

Meeting these criteria signals readiness to enter Phase 2B (storage + reward integration).
