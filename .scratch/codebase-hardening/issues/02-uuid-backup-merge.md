# 02: UUID-Authoritative Deduplication for Backup Merge

**What to build:**
Allow backup merge (`RestoreStrategy.MERGE_KEEP_EXISTING`) to preserve distinct counters that happen to share a common name (such as separate "Haircut", "Dentist", or "Sobriety" trackers from different devices). Deduplication must rely strictly on immutable UUIDs, eliminating silent data loss caused by normalized name matching.

**Blocked by:**
None (can start immediately)

**Status:**
ready-for-agent

- [x] In `CountUpStore.restoreBackupPayload()`, update the merge strategy filter to deduplicate incoming items strictly by `item.id !in existingIds`.
- [x] Add unit tests covering backup merge with duplicate names but distinct UUIDs, verifying both records are retained.
- [x] Verify existing items with identical UUIDs are still preserved without being duplicated.
