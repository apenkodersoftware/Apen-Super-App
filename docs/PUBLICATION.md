# Public R3 source and review artifacts

Published 30 October 2026 to apenkodersoftware/Apen-Super-App.

The release APK is the unchanged owner-review build tested on emulator-5554 on 29 October. It is debug signed and not approved for customer distribution. Dairy, Rental, Restaurant and Retail are included; Poultry remains a planned next scenario.

The public source excludes all signing keys, private databases, encrypted customer backups, recovery secrets, local.properties, build caches and generated APKs. The original local source archive contained a review debug keystore; that key is deliberately omitted from this public source archive. Runtime source files are byte-identical to the tested source. Publication changes are limited to documentation, ignore rules, and generating a disposable CI debug key.

## Build locally

Use JDK 21 and the Android SDK platform android-36.1/build-tools 36.0.0. Set ANDROID_HOME or your ignored local.properties sdk.dir. From the repository root generate your own ignored debug key:

```
keytool -genkeypair -keystore debug.keystore -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 365 -dname "CN=Android Debug,O=Android,C=US"
```

Then run the Gradle wrapper (gradlew.bat on Windows):

```
./gradlew codeQualityCheck lintDebug testDebugUnitTest assembleDebug --no-daemon --max-workers=2
```

A locally generated key has a different certificate from the downloaded review APK. It does not reproduce the release asset signature and cannot update an installation signed with that other key. No production signing key is supplied. The release build has a debug-key fallback for compilation checks; customer distribution requires an approved real signing/update plan. The Firebase distribution workflow is manual and requires separate real credentials; this publication does not run it.

Native acceptance is constrained to emulator-5554 for this project. The verification application flag creates a separate disposable package on that same device; no verification APK is a release asset. Never clear the main package to run destructive fixtures. Recovery evidence describes same-device recovery, not a second-device result.

## Review evidence

See R3-HANDOVER.md, R3-PRODUCT-SUMMARY.md, PLAN.md, POULTRY-NEXT-ITERATION.md and evidence/. The handover records the 29 October local test results; GitHub Actions results are separate. The original APK manifest records local archive identities, while the publication manifest records the sanitized source archive and uploaded release assets. No customer data or private recovery working files are published.
