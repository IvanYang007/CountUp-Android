# 04: Release Keystore Password Scrub & Rotation

**What to build:**
Secure release credentials by preparing instructions/scripts to scrub historical commits containing `keystore/keystore-pass.txt` (commit `32f6f36`) from the Git history using `git-filter-repo` before making the repository public, and generating a fresh release signing key.

**Blocked by:**
None (can start immediately)

**Status:**
ready-for-agent

- [x] Provide script or documented procedure to purge `keystore/keystore-pass.txt` from all Git packfile refs and tags.
- [x] Untrack committed static files in `artifacts/` from the Git index while retaining local ignored copies.
- [x] Document key generation command for production key rotation if releasing to Google Play.
