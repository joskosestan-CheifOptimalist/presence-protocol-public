# S23 Missing Chars Investigation

Observed issue:
- S23 showed:
  - PP_HANDSHAKE SERVICES_DISCOVERED
  - PP_HANDSHAKE FAIL reason=missing chars

Investigation focus:
1. inspect service and characteristic lookup in PresenceGattClient.onServicesDiscovered
2. confirm PresenceGattUuids match installed server/client builds
3. compare successful A17 path against failing S23 path
4. confirm server service registration order and timing
5. confirm both HELLO_CHAR_UUID and REPLY_CHAR_UUID are present when S23 queries services

Target outcome:
- isolate why S23 sometimes fails characteristic lookup after services discovered
- document exact fix path without rewriting handshake architecture

## Resolution update — role-gated fix confirmed

Confirmed findings:
- The original S23 missing-characteristic failure had two causes:
  1. GATT server readiness was being treated as complete immediately after addService(), before onServiceAdded().
  2. The server APK was also initiating outbound GATT client connections toward the client APK, which does not expose the Presence GATT service.

Fixes applied:
- PresenceGattServer now sets readiness from onServiceAdded() instead of assuming addService() completion is sufficient.
- PresenceDiscoveryController now prevents outbound PresenceGattClient initiation when packageName ends with ".server".

Verified outcome:
- Server package (S23) now reaches:
  - GATT_SERVER_SERVICE_ADDED status=0
  - PP_HANDSHAKE SERVER_READY true
- Client package (A17) successfully completes:
  - GATT_CONNECTED
  - SERVICES_DISCOVERED
  - SERVICE_LOOKUP serviceFound=true replyFound=true helloFound=true
  - HANDSHAKE_COMPLETE
- Reverse-path false failure ("missing chars" on S23) no longer occurs.

Suggested run classification:
- RUN_011 = PASS (server readiness fix + role-gated handshake stabilization)
