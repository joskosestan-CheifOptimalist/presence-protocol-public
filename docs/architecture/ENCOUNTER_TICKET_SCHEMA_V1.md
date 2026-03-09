# Encounter Ticket Schema V1

Purpose: represents a completed Presence Protocol handshake.

Required fields:
- encounter_id
- device_a_ephemeral_key
- device_b_ephemeral_key
- hello_hash
- reply_hash
- handshake_timestamp
- nonce
- protocol_version
- app_version
- device_a_signature
- device_b_signature

Validation rules:
- both device keys must exist
- hello/reply hashes must exist
- signatures must verify
- nonce must be unique

Replay protection:
- encounter_id uniqueness
- nonce uniqueness
- cooldown enforcement
