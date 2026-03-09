# Handshake to Ticket Mapping

encounter_id
- generated UUID at handshake completion

device_a_ephemeral_key
- local ephemeral identity used in handshake

device_b_ephemeral_key
- remote ephemeral identity learned from handshake

hello_hash
- hash of hello payload written during GATT exchange

reply_hash
- hash of reply payload received during GATT exchange

handshake_timestamp
- timestamp recorded at successful handshake completion

nonce
- new random nonce generated for ticket uniqueness

protocol_version
- presence_v1

app_version
- current app build/version

device_a_signature
- local signature over canonical ticket payload

device_b_signature
- remote signature or deferred placeholder until dual-sign path is finalized
- local signature over canonical encounter ticket payload

device_b_signature
- remote signature over canonical encounter ticket payload
- if dual-sign path is not yet fully available in app, document temporary placeholder strategy before verifier upload

Notes
- ticket must only be created after PP_HANDSHAKE HANDSHAKE_COMPLETE
- discovery-only or partial GATT events must never create a ticket
- RUN_007 remains the reference handshake path for this mapping

## Signature Verification Status (Alpha)

Current implementation performs **local self-consistency verification only**.

Specifically:
- `deviceASignature` is verified against the local ephemeral public key.
- `deviceBSignature` is also verified using the same local key.

This currently gives:
- deterministic local verification
- ticket generation continuity
- replay/suppression-friendly consistency during alpha testing

This is **not yet true bilateral cryptographic verification** between peers.

True bilateral verification will require:
1. exchange of peer ephemeral public keys
2. verification of `deviceBSignature` against the peer public key
3. challenge/response binding so signatures are tied to the actual counterparty
4. protocol evidence proving the peer key used for verification was received over the handshake path
