# QA Status Summary

| Run ID  | Timestamp | Branch                    | HEAD Commit                           | Result    | Evidence Files                                     | Learnings       |
|---------|-----------|---------------------------|---------------------------------------|-----------|----------------------------------------------------|------------------|
| RUN_000 | [timestamp] | phase2a-gatt-transport    | 78b0a676bee9527c0e8a777bee247f536805db44 | [STATUS] |
| RUN_001 | [timestamp] | phase2a-gatt-transport    | 78b0a676bee9527c0e8a777bee247f536805db44 | [STATUS] |
| RUN_002 | [timestamp] | phase2a-gatt-transport    | 78b0a676bee9527c0e8a777bee247f536805db44 | [STATUS] |

## RUN_003 Acceptance Criteria
- Scan matching must trigger on discovery.
- GATT connection attempt must succeed once a match occurs.
- Connected state, Service discovery, and optional Hello/Reply must be logged.


## RUN_006
- Status: PARTIAL PASS
- Type: observability + callback instrumentation
- Result:
  - handshake lifecycle logging added
  - callback chain confirmed beyond `CONNECT_START`
  - asymmetric / `BOTH` mode issues still present
  - S23 client observed `status=133` in some attempts
- Conclusion: diagnostic success, not yet final clean reference pass


## RUN_007
- Status: PASS
- Type: role-separated validation
- Server device: S23
- Client device: A17
- Evidence:
  - `docs/qa_logs/RUN_007/run007b_s23.log`
  - `docs/qa_logs/RUN_007/run007b_a17.log`
- Key result: end-to-end handshake confirmed in role-separated mode
- Note: RUN_005 remains partial-pass historical evidence; RUN_006 is observability partial-pass; RUN_007 is the first clean role-separated full pass
