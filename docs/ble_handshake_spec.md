# Presence Protocol — BLE GATT Handshake Spec (v0)

This document is the **source of truth** for the BLE discovery + GATT handshake used by Presence Protocol.
Implementation references:
- `mobile/data-ble/.../PresenceGattUuids.kt`
- `mobile/core-common/.../PresenceCborPackets.kt`

---

## 1) Roles

Devices operate in one of three roles:
- **CLIENT_ONLY**: scans + connects and performs GATT writes/receives notifications.
- **SERVER_ONLY**: advertises + hosts GATT server, receives HELLO, notifies REPLY, receives RESULT.
- **BOTH**: does both roles (useful for field testing).

Current configuration (build-time constant):
- `mobile/app/src/main/java/com/presenceprotocol/app/BleConfig.kt`

---

## 2) GATT UUIDs (locked)

Service and characteristic UUIDs are fixed:

### Service UUID
- **PRESENCE_SERVICE_UUID** = `7d3a2d6b-9b7a-4f2a-9e5e-0c9d6f1b1c01`

### Characteristics
- **HELLO_CHAR_UUID** (client → server **WRITE**)  
  `7d3a2d6b-9b7a-4f2a-9e5e-0c9d6f1b1c02`

- **REPLY_CHAR_UUID** (server → client **NOTIFY**)  
  `7d3a2d6b-9b7a-4f2a-9e5e-0c9d6f1b1c03`

- **RESULT_CHAR_UUID** (client → server **WRITE**)  
  `7d3a2d6b-9b7a-4f2a-9e5e-0c9d6f1b1c04`

---

## 3) Handshake Sequence

### Message flow
1. **Discovery**
   - SERVER advertises `PRESENCE_SERVICE_UUID`
   - CLIENT scans for `PRESENCE_SERVICE_UUID`

2. **Connect**
   - CLIENT connects to SERVER’s GATT service.

3. **HELLO (client → server)**
   - CLIENT generates:
     - `sid` (sessionId bytes)
     - `nonce` (bytes; recommended 32 bytes)
     - `pk_c` (client Ed25519 public key bytes)
     - `ts` (timestampSeconds)
   - CLIENT writes CBOR-encoded `HelloPacket` to `HELLO_CHAR_UUID`.

4. **REPLY (server → client)**
   - SERVER validates HELLO (basic size/version checks).
   - SERVER prepares `ReplyPacket`:
     - copies `sid`, `nonce`
     - adds `pk_s` (server Ed25519 public key bytes)
     - sets `code` (statusCode int)
     - computes `sig` (signature bytes)
   - SERVER NOTIFYs CBOR-encoded `ReplyPacket` on `REPLY_CHAR_UUID`.

5. **CLIENT verify**
   - CLIENT verifies `sig` using `pk_s` and the agreed signing bytes (see §5).
   - CLIENT applies cooldown / replay rules (see §6).
   - CLIENT persists encounter if accepted.

6. **RESULT (client → server)**
   - CLIENT writes an optional result payload to `RESULT_CHAR_UUID`
   - (v0: format is implementation-defined; keep stable once chosen)

---

## 4) CBOR Payload Schemas

Encoding/decoding is implemented in:
`PresenceCborPackets.kt`

### 4.1 HelloPacket (client → server)

CBOR map keys:

| Key | Type | Meaning |
|---|---|---|
| `v` | int | protocol version |
| `sid` | bstr | session id bytes |
| `nonce` | bstr | nonce bytes (recommended 32 bytes) |
| `pk_c` | bstr | client public key bytes |
| `ts` | int | timestampSeconds |

### 4.2 ReplyPacket (server → client)

CBOR map keys:

| Key | Type | Meaning |
|---|---|---|
| `v` | int | protocol version |
| `sid` | bstr | session id bytes (echo) |
| `nonce` | bstr | nonce bytes (echo) |
| `pk_s` | bstr | server public key bytes |
| `sig` | bstr | server signature bytes |
| `code` | int | status code (0 = OK; other values reserved) |

---

## 5) Signature Rules (Ed25519)

The REPLY contains `sig` which must be verified by the CLIENT.

### Signing input (v0 rule)
To avoid ambiguity and future rewrites, v0 defines the signature as:

**Ed25519 signature over the CBOR bytes of the ReplyPacket with the `sig` field treated as empty.**

Concretely:
- Build the ReplyPacket map with all fields populated
- Set `sig` = empty byte string (`h''`)
- Encode CBOR to bytes
- Sign those bytes with the server’s Ed25519 private key
- Replace `sig` with the signature bytes
- Send final encoded ReplyPacket

**Client verification does the same reconstruction:**
- Decode ReplyPacket
- Rebuild bytes with `sig = empty`
- Verify signature against `pk_s`

> This rule prevents circular “signing the signature” and ensures deterministic verification.

(If implementation currently differs, update either code or this spec so both match.)

---

## 6) Cooldown + Replay Resistance (v0)

### 6.1 Cooldown keying
Primary: `pk_s` (server public key bytes)  
Secondary (optional): BLE address (best-effort, not authoritative)

### 6.2 Cooldown rule
If the client has accepted an encounter with the same `pk_s` within `COOLDOWN_SECONDS`,
the encounter must be rejected as **cooldown blocked**.

### 6.3 Replay rule
A REPLY is rejected if:
- `sid` does not match the current in-flight session, OR
- `nonce` does not match the HELLO nonce for that session, OR
- timestamp is outside allowed skew window (recommended ±300s), OR
- cooldown blocks it.

---

## 7) Status Codes (ReplyPacket.code)

- `0`: OK / verified reply available
- non-zero: reserved (future use). Client should treat non-zero as failure and log.

---

## 8) Observability (debug)

Recommended debug events (no secrets in logs):
- discovered_peer
- gatt_connected / disconnected
- hello_written (length only)
- reply_notified (length + code)
- sig_verified_ok / sig_verified_fail
- cooldown_allowed / cooldown_blocked

Never log private keys.

---

## 9) Compatibility Notes
- BLE behavior varies across Android OEMs and OS versions.
- Keep timeouts bounded; avoid tight retry loops.
- Prefer deterministic signing bytes (see §5).
