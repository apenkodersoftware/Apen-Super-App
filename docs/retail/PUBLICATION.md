# Source publication - 23 October 2026

Destination: https://github.com/apenkoder-software/malimali-App

Branch: `feature/malimali-foundation`. This initial publication contains a source snapshot, with provenance retained in the README and implementation scope. It does not publish FasMova's historical Git objects.

The previously verified source package contains 129 files and has SHA-256 `4eb7ac925bb82a7e20357b046cead7ad9386ce9548c8c04a148da590dae9433d`. Those files were hash-checked before preparing this publication. Publication changes only update the repository references in README.md and IMPLEMENTATION.md and add this document. App source, dependencies, tests and workflow definitions are unchanged from that verified package. Git's inherited text attributes normalize text line endings; shell entry points retain executable permission.

Local checks completed on the packaged implementation:

- `codeQualityCheck`: passed.
- `testDebugUnitTest`: 77 tests in 17 suites; zero failures, errors or skips. This includes 17 retail regression cases.
- `lint`: passed with 111 warnings and zero errors.
- `assembleDebug`: passed; application ID `com.apenkoder.malimali`, version 0.1.0.
- Debug APK signature verification: passed with the public Android Debug key.

These are local results, not GitHub Actions results. Device/emulator acceptance, release signing, production distribution and real-shop readiness remain unverified. See IMPLEMENTATION.md for deferred requirements. An inherited receipt label still says TOTAL PAID for credit sales and needs correction before deployment.

The repository excludes generated APKs, source ZIPs, build caches, local configuration, private signing material and raw local verification logs. The inherited public debug.keystore is included solely for reproducible development builds.

The inherited CD workflow distributes on pushes to `main`. This publication uses a separate branch and does not authorize distribution. Review or replace the inherited distribution setup before creating or merging to `main`.
