# Hello Reply Hash Capture Plan

helloHash capture point:
- PresenceGattClient.writeHello(gatt)
- compute hash from the actual payload bytes before or at write time
- store value for EncounterTicket generation

replyHash capture point:
- PresenceGattClient.onCharacteristicChanged(...)
- compute hash from received reply bytes
- store value for EncounterTicket generation

Temporary implementation rule:
- hash values may be stored in-memory on the client path first
- no backend dependency required for RUN_010

Target outcome:
- EncounterTicket JSON logs real helloHash and real replyHash values
