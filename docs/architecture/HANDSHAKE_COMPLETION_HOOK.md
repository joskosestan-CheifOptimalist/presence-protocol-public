# Handshake Completion Hook

Hook location:
- PresenceHandshakeCoordinator.markComplete(peerId)

Reason:
- this is the first confirmed success point after full handshake completion

Required action at hook:
1. collect available handshake values
2. build EncounterTicket
3. serialize EncounterTicket
4. write temporary log output
5. leave upload for later verifier phase

Do not trigger ticket creation from:
- discovery only
- connect start
- service discovery only
- partial or failed handshake states

Reference:
- RUN_007 is the baseline successful path
