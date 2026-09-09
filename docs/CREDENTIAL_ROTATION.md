# Credential Rotation & History Cleansing Guide

This document outlines the procedure to purge the historical keystore password commit from repository history and generate a fresh production signing key before public distribution or publishing to the Google Play Store.

---

## 1. Purging Historical Password Exposure from Git History

In commit `32f6f36211113906d4bc4f8c03e519b711b198e1`, `keystore/keystore-pass.txt` was tracked before being removed from tracking in `625f2c387078c9ed3ff583d715bafa1eecb19d28`. While untracked in `HEAD`, the password string remains present in historical git packfile objects.

### Recommended Tool: `git-filter-repo`

Ensure you have a clean working tree and backup of the repository before running history rewrites.

```bash
# Install git-filter-repo (Python-based)
pip install git-filter-repo

# Scrub keystore-pass.txt across all historical branches and tags
git filter-repo --invert-paths --path keystore/keystore-pass.txt --force
```

### Alternative: BFG Repo-Cleaner

```bash
bfg --delete-files keystore-pass.txt
git reflog expire --expire=now --all && git gc --prune=now --aggressive
```

---

## 2. Untracking Committed Artifacts from Git Index

The `.gitignore` specifies `artifacts/`, but historical binary PNG/XML/HTML files remain tracked in the Git index. To untrack them without deleting your local copies:

```bash
git rm --cached -r artifacts/
git commit -m "chore: untrack local artifacts directory from git index"
```

---

## 3. Generating a Fresh Production Signing Key

If release APKs/AABs signed with the old key have not yet been distributed to Google Play (or if using Google Play App Signing with upload key reset):

```bash
# Generate a new production RSA 4096-bit release keystore
keytool -genkeypair -v \
  -keystore keystore/countup-release.jks \
  -alias countup \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000 \
  -storetype PKCS12
```

### Safe Password Management

Do **not** commit password files to git. Use one of the supported loading mechanisms configured in `app/build.gradle.kts`:

1. **Environment Variable (Recommended for CI)**:
   ```bash
   export COUNTUP_KEYSTORE_PASS="your_secure_password"
   ```
2. **Local Properties (Recommended for local builds, gitignored)**:
   Add to `local.properties`:
   ```properties
   countup.keystore.pass=your_secure_password
   ```
