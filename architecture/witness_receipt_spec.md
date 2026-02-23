# Witness Receipt Specification

## Canonical Payload
```
{
  "ver": 1,
  "epoch": <unix epoch seconds>,
  "bucket": <5s bucket value>,
  "eid_a": <16-byte hex>,
  "eid_b": <16-byte hex>,
  "na": <12-byte nonce hex>,
  "nb": <12-byte nonce hex>,
  "caps_a": <uint8 bitmask>,
  "caps_b": <uint8 bitmask>,
  "rssi": <int8 clamped -100..-20>,
  "met": {
    "lat_bucket": optional,
    "wifi_hash": optional
  }
}
```
- Encoding: CBOR with fixed field order above.
- `enc_id = BLAKE2b256(CanonicalEncode(payload))`.

## Signatures
- Long-term Ed25519 keys per device (deviceKeyId).
- `sig_a = Sign(enc_id, LT_sk_A)`, `sig_b = Sign(enc_id, LT_sk_B)`.
- Proof bundle: `{ enc_id, payload, lt_pk_a, lt_pk_b, sig_a, sig_b, deviceKeyId_a, deviceKeyId_b }`.

## Encounter Policies
- Time window: `abs(now - epoch) <= 10s`.
- Cooldown: reject if pair seen within 45 minutes.
- Dedupe: backend enforces unique `enc_id`.
- Privacy: only hashed bucket metadata, no raw GPS/BLE IDs stored.

## Cardano Alignment
- Ed25519 signatures match Cardano’s native verification.
- `enc_id` will be the commitment anchored on-chain.
- Later: include Cardano-native witness fields without changing base structure.
