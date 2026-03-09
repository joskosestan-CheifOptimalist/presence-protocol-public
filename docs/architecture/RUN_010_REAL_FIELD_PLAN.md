# RUN_010 Real Field Plan

Goal:
Produce the first EncounterTicket pass with richer real mapped values instead of mostly placeholders.

Primary targets:
- appVersion from real build metadata
- helloHash from actual hello payload
- replyHash from actual reply payload
- real handshake completion timestamp
- reduced placeholder usage in logged EncounterTicket JSON

Acceptance criteria:
- handshake completes on live devices
- PP_TICKET GENERATED appears
- PP_TICKET JSON appears
- appVersion is not hardcoded "dev"
- helloHash is not hello_hash_placeholder
- replyHash is not reply_hash_placeholder

Reference runs:
- RUN_007 = handshake baseline
- RUN_008 = stub ticket generation
- RUN_009 = full ticket JSON logging
