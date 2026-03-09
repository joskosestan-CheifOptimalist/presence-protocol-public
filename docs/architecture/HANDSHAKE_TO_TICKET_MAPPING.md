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
