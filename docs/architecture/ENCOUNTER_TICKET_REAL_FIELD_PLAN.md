# Encounter Ticket Real Field Plan

Current verified state:
- RUN_010 proved real values are now logged for:
  - appVersion
  - helloHash
  - replyHash

Still placeholder or incomplete:
- deviceAEphemeralKey
- deviceASignature
- deviceBSignature

Current real-enriched fields:
- deviceBEphemeralKey
- helloHash
- replyHash
- handshakeTimestamp
- nonce
- protocolVersion
- appVersion

Next realistic replacement target:
- deviceAEphemeralKey

Deferred for now:
- full dual-sign implementation
- verifier upload
- chain settlement

Reference runs:
- RUN_007 = handshake baseline
- RUN_008 = first stub ticket generation pass
- RUN_009 = first full ticket JSON logging pass
- RUN_010 = first real-field EncounterTicket pass
