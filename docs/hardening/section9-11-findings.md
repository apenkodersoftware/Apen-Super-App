# FasMova POS - Sections 9, 10 and 11 findings

Date: 2026-10-08 - Branch: `feature/onboarding-payments-darkmode-swahili`
Mode: **findings only, nothing in the app was fixed.** No emulator was used (Lesson 7B: no "emulator idle, go" given).
Severity is High / Medium / Low; "hardening note" means no named attacker path, so it does not count toward severity totals.

## Section 9 - live runtime debugging

| Item | Status |
|---|---|
| 9.1 logcat crash/ANR capture | **unverified** (needs emulator) |
| 9.2 meminfo / leak profiling | **unverified** (needs emulator; LeakCanary is wired as `debugImplementation`) |
| 9.3 ANR / deadlock | **unverified** (needs emulator or Profiler) |
| 9.4 Manifest / Play data-privacy first pass | done, see below |
| 9.5 `lintRelease`/`lintVitalRelease`, `testReleaseUnitTest` | `lintVitalRelease` **pass** (a first run failed on a transient dependency-resolution error for `kotlin-stdlib-jdk7:1.8.22`, a retry passed). `testReleaseUnitTest` **does not exist** in this project (see Section 10). |
| 9.6 Layout Inspector | **unverified** (manual, Android Studio) |
| 9.7 Battery / device-condition simulation | **N/A** (the app does not read battery state) |

### 9.4 Merged release manifest (`processReleaseMainManifest`)
Permissions: `INTERNET`, `ACCESS_NETWORK_STATE`, `READ_GSERVICES`, plus the library-generated `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`. None are declared by the app itself.
- **Medium (hardening note for Play data safety): `INTERNET`/`READ_GSERVICES` come from `firebase-ai` and `firebase-appcheck-recaptcha`.** Nothing in `src/main` uses Firebase, Retrofit or OkHttp (no network call found), and `google-services.json` is absent (Lesson 12/13). Declaring a network-capable app while the Terms say "data stays on device" invites a Data Safety mismatch. Comment out `firebase.ai` and `appcheck-recaptcha` until used.
- Exported components: `MainActivity` (launcher, intentional) and `androidx.profileinstaller.ProfileInstallReceiver` (library, protected by `android.permission.DUMP`, standard and acceptable). All others are `exported="false"`. No deep links, no custom intent filters.

## Section 10 - preflight.sh (v1.1), run 2026-10-08

Saved as `preflight.sh`; output goes to `debug_logs/` (now in `.gitignore`).

| Step | Result | Triage |
|---|---|---|
| 1 lintVitalRelease | FAIL on first run | Root error: could not resolve `kotlin-stdlib-jdk7:1.8.22`. Transient; rerunning `./gradlew lintVitalRelease` passes. |
| 2 targetSdk >= 36 | OK (36) | |
| 3 cleartext | WARN (2 hits), manifest OK | **False positives:** both hits are binary matches inside the two JPGs. |
| 4 device identifiers | OK | |
| 5 debug deps isolated | OK | LeakCanary is `debugImplementation`. |
| 6 secrets | WARN (14 hits) | **Noise:** all comments, `PasswordVisualTransformation`, `SecretKeyFactory` etc. No literal secret found. |
| 6 committed key files | FAIL (1) | **False positive:** `.env.example` matches the script's `(^|/)\.env` pattern. It is an intentional template. Real `.env` is ignored. |
| 7 debuggable / backup | OK / WARN | No hardcoded `debuggable`. `allowBackup="true"`, see 11.4. |
| 8 release unit tests | FAIL | `Task 'testReleaseUnitTest' not found`. Only `testDebugUnitTest` is registered in this AGP 9.1.1 project. I did not establish why. |

Script defects to fix in the doc (v1.2): step 6 should exclude `.env.example` (e.g. `(^|/)\.env($|\.local|\.prod)`), steps 3 and 6 should use `grep -I` to skip binaries, and step 8 should fall back to `testDebugUnitTest` (or enable release unit tests deliberately) when the release task is absent. The remaining true result is: **no real preflight failures**, but two scripted checks cannot go green until the script or project is adjusted.

## Section 11 - security audit (static)

Not checked (needs a device, release APK or backend): APK Analyzer decompile (11.6), on-device logout data clearing, backup/restore behaviour. Listed as unverified.

| # | Item | Finding or note | Attacker / boundary / result | Severity |
|---|---|---|---|---|
| 11.1 | Hardcoded secrets | None found (preflight step 6 reviewed) | - | pass |
| 11.1 | Key files in git | Only `debug.keystore` (intended) and `.env.example` | - | pass |
| 11.1 | **Seed accounts not gated by build type** | `seedInitialDataIfEmpty()` creates Owner/Manager/Cashier with PINs 1234/2222/0000 in every build, with no `BuildConfig.DEBUG` guard (`FasMovaPOSRepository.kt:44-84`) | Anyone who picks up an installed release build signs in as Owner with a guessable PIN | **High** |
| 11.1 | Stack traces / raw errors to users | None rendered. But the ViewModel has no exception handling, so a failure crashes the app (see Section 8) | - | hardening note |
| 11.1 | TODO on auth/validation | Only the backup-rules TODO (see 11.4) | - | see 11.4 |
| 11.2 | Exported components / deep links | Only the launcher activity and the DUMP-protected profile receiver. No deep links, share or file handlers | - | pass |
| 11.3 | WebViews | None in the app | - | N/A |
| 11.4 | **Backup exposes credentials** | `allowBackup="true"` and empty `data_extraction_rules.xml`/`backup_rules.xml`, so the Room DB (staff `pinHash` and `pinSalt`) is included in cloud backup and device transfer | Anyone with the user's Google backup or a transferred device gets hashes plus salts; a 4-digit PIN falls to offline brute force in seconds. Also contradicts Terms section 2 | **High** |
| 11.4 | **No PIN attempt limiting** | `signIn` and the PIN-only `verifyStaffPinForBranch` (`:614-642`) have no lockout, delay or counter | Someone holding the device guesses 10,000 PINs on the login or cashier-switch dialog, unrestricted | **Medium** |
| 11.4 | PIN hashing | PBKDF2, random 16-byte salt, constant-time compare, 20k iterations (SHA-256) or 60k (SHA-1 fallback). Documented as modest by design | Offline attack is trivial regardless, since the PIN space is 10^4 | hardening note |
| 11.4 | Session storage | DataStore holds only `logged_in_user_id`, no token or PIN. `restoreSession` re-logs the user without a PIN, and the id is included in backups | Restored backup resumes the session | hardening note |
| 11.4 | Logging | No `Log.*`, `println` or `printStackTrace` in `src/main` | - | pass |
| 11.4 | Logout clears caches | `signOut` clears the session id only; no per-user data exists locally beyond the shared DB | - | unverified (device) |
| 11.5 | Backend / tenant isolation | No backend, Firebase unused, Sync screen is simulated | - | N/A now; **must be designed before sync goes live** (branch scoping today is a client-side filter) |
| 11.6 | **Role enforcement is UI-only** | Roles (OWNER/MANAGER/CASHIER) only hide controls (e.g. `StaffScreen.kt:88`). `addNewStaffUser`, expense, stock and payment functions in the ViewModel/repository check no role | A cashier-level user who reaches a hidden control (or a future bug that exposes one) can create staff or alter stock; no server to stop it. Escalate to Gate 2 (auth) | **Medium** |
| 11.6 | **Mobile money is simulated** | `newPaymentReference` fabricates `MPESA-xxxx`/`AIRTEL-xxxx` references (`FasMovaPOSRepository.kt:471`); no provider callback or verification | A cashier can record a mobile-money sale as paid without any money moving. Fine for a demo, a blocker for real use. Escalate to Gate 2 (payment) | **High** (before any real-money release) |
| 11.6 | Replay / idempotency | None: each checkout creates a new sale id (see Section 8 double-submit) | Double-tap writes two sales | **High** (already tracked) |
| 11.6 | Amount validation | `processSale` uses `require(...)` for sum and credit rules; negative/zero quantities are not validated in the repository | Crash instead of a clean rejection | hardening note |
| 11.6 | Kill-switch / licensing | None | - | N/A (decide direction before adding) |
| 11.7 | Cost / availability | No paid calls (Firebase AI unused). No budget needed until a backend exists | - | N/A now |
| 11.8 | Dependencies pinned | All via version catalog, no `+` or `latest`. `okhttp 4.10.0`, Compose BOM `2024.09.00` and `lifecycle 2.8.7` are old; Dependabot now covers them | - | hardening note |
| 11.8 | CI secrets | `ci.yml` uses no secrets and the `pull_request` trigger. Secrets only in `cd.yml` on `main` | - | pass |
| 11.8 | Actions pinned by tag, not SHA | `actions/*@v4`, `w9jds/firebase-action@v2.6.0` | A compromised third-party tag runs with the Firebase key on `main` | hardening note |
| 11.8 | **Release signed with debug key** | `assembleRelease` falls back to `debug.keystore` unless `KEYSTORE_PATH` is set. Branch protection on `main` is not confirmed | Anyone can re-sign an APK with the public debug key; unreviewed pushes to `main` ship to testers | **Medium** (Play blocker too) |
| 11.8 | Test what you ship | Testers will get a debug-signed APK, not the eventual Play AAB | - | hardening note |

### Escalate-Severity items for Gate 2 (per ADR-002)
Auth and payment findings: seed accounts, PIN lockout, UI-only role enforcement, simulated mobile money, double-submit, backup of credentials.

### Suggested triage (nothing fixed yet)
1. Gate the seed accounts (debug only, or first-run owner setup) and exclude the database from backup. Both touch auth/data, so they go through Gate 2.
2. Double-submit guard on checkout.
3. PIN attempt limiting.
4. Comment out unused Firebase dependencies (also removes `INTERNET`).
5. Decide the real payment-confirmation flow before enabling real mobile money.
