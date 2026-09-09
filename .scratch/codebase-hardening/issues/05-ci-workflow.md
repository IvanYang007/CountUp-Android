# 05: Continuous Integration Workflow with GitHub Actions

**What to build:**
Automate validation of JVM unit tests, Android Lint, and release APK assembly on every pull request and commit to `main` via a lightweight GitHub Actions workflow.

**Blocked by:**
None (can start immediately)

**Status:**
ready-for-agent

- [x] Create `.github/workflows/ci.yml` running on `ubuntu-latest` with Temurin JDK 17.
- [x] Configure workflow steps to execute `./gradlew test lintDebug assembleRelease`.
- [x] Ensure Gradle caching is enabled for fast subsequent test runs.
