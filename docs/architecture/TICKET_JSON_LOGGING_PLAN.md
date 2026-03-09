# Ticket JSON Logging Plan

Goal:
Log the full EncounterTicket JSON after handshake completion.

Log trigger:
- PresenceHandshakeCoordinator.markComplete(peerId)

Required output:
- encounterId
- deviceAEphemeralKey
- deviceBEphemeralKey
- helloHash
- replyHash
- handshakeTimestamp
- nonce
- protocolVersion
- appVersion
- deviceASignature
- deviceBSignature

Temporary rule:
- log locally before any backend upload exists

Target run:
- RUN_009 = first real-field ticket JSON logging validation
