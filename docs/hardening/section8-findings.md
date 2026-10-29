# FasMova POS - Section 8 pre-demo hardening findings

Date: 2026-10-08 - Branch: `feature/onboarding-payments-darkmode-swahili` (5ca08a4)
Mode: **findings only, nothing fixed.** Static pass (file inspection, code reading, existing JVM tests).
**No emulator was used** (Lesson 7B: no explicit "emulator idle, go"). Every cell that needs a device is marked `unverified`.
Verification legend: **JVM** = passing Compose/Robolectric test, **code** = read in source, **device** = run on emulator, **unverified**.
Severity is High / Medium / Low (ATLAS §26a scale not available in this repo; map as needed).

## 8.1 Launcher icons

| Check | Result |
|---|---|
| Density buckets mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi | All present (`ic_launcher.webp` and `ic_launcher_round.webp`) |
| Size vs bucket | Monotonic (1.4 KB mdpi to 5.6 KB xxxhdpi; round 2.6 to 11.4 KB). None near 50 KB. Pass |
| Adaptive icon (`mipmap-anydpi-v26`) | Present, with background, foreground and monochrome. Pass |
| **Foreground source** | `ic_launcher_foreground.xml` is a layer-list wrapping `drawable/img_app_icon_1785386840128.jpg` (341 KB JPEG). **Medium.** An opaque JPG cannot have transparency, so the icon is a flat square inside the mask, and it is decoded at full size. |
| **Monochrome layer** | Reuses the same coloured JPG as the foreground. **Low-Medium.** Android 13 themed icons will render a solid blob. |

Fix path (deferred): regenerate with Image Asset Studio from a vector or transparent PNG source.

## 8.2 Drawable density placement

| File | Size | Verdict |
|---|---|---|
| `drawable/img_app_icon_1785386840128.jpg` | 341 KB | **Not acceptable.** It is an icon source (see 8.1). Move to a vector, or to density buckets with a transparent PNG/WebP. |
| `drawable/img_pos_hero_1785386853893.jpg` | 807 KB | **Single high-res source scaled at runtime** (used once, `InventoryScreen.kt:111`). Acceptable for a hero image, but 807 KB is heavy. Convert to WebP (**Low**). |
| `drawable-nodpi/apenkoder_mark.png` (219 KB), `apenkoder_splasher.jpg` (203 KB) | - | Correctly in `nodpi`. |

## 8.3 State-holder / ViewModel presence

Inventory (all files read in full except where noted): 10 screens under `ui/screens`, 3 onboarding/auth screens, plus `MainActivity.kt`.
Method: grep for Room/DataStore/SharedPreferences imports and `collectAsState*` in each file, then read.

| Screen file | Direct state read in Composable? | ViewModel / state holder | Verdict |
|---|---|---|---|
| `ui/screens/POSScreen.kt` (749 lines) | None found | Shared `FasMovaPOSViewModel` (state hoisted via parameters from `MainActivity`) | Pass |
| `ui/screens/InventoryScreen.kt` (642) | None found | Shared `FasMovaPOSViewModel` | Pass |
| `ui/screens/CustomersScreen.kt` (397) | None found | Shared `FasMovaPOSViewModel` | Pass |
| `ui/screens/SuppliersScreen.kt` (356) | None found | Shared `FasMovaPOSViewModel` | Pass |
| `ui/screens/StaffScreen.kt` (308) | None found | Shared `FasMovaPOSViewModel` | Pass |
| `ui/screens/AnalyticsScreen.kt` (286) | None found | Shared `FasMovaPOSViewModel` | Pass |
| `ui/screens/ExpensesScreen.kt` (258) | None found | Shared `FasMovaPOSViewModel` | Pass |
| `ui/screens/SalesHistoryScreen.kt` (255) | None found | Shared `FasMovaPOSViewModel` | Pass |
| `ui/screens/SyncEngineScreen.kt` (240) | None found | Shared `FasMovaPOSViewModel` | Pass |
| `ui/screens/LoginScreen.kt` (171) | None found | Shared `FasMovaPOSViewModel` (`authState`, `isAuthLoading`, `authError`) | Pass |
| `onboarding/TermsScreen.kt` (138) | None found | None. Hoisted callbacks from `MainActivity`. Local `rememberSaveable` only. | Pass |
| `onboarding/SplashScreen.kt` (49) | None found | None needed (static) | Pass |
| **`MainActivity.kt`** (372) | **Yes.** `themePrefs.themeMode.collectAsStateWithLifecycle` (`:73`), `termsPrefs.hasAcceptedTerms.collectAsStateWithLifecycle` (`:120`), plus `LanguageSettings` reading SharedPreferences via `AppLanguage.kt:29,38` | No ViewModel for theme/terms/language | **Flag for Gate 1/2** |

Architecture findings to flag (not fixed, per the rules of this pass):
- **Medium - God ViewModel.** One `FasMovaPOSViewModel` (576 lines, ~40 exposed flows) serves every screen, and `MainActivity` collects all of them at `:166-198`.
- **Medium - preference reads outside a state holder.** Theme, Terms and language are read directly in `MainActivity`/helpers.

## 8.4 Screen-by-screen runtime behaviour

Cells: result (how verified).

| Screen | Empty state | Rotation | Process death | Notes |
|---|---|---|---|---|
| POS | pass (code): `POSScreen.kt:284` has an empty block. Cart empty state handled (`:365`, `:436`) | pass (code): cart, payment, search and filter are in the VM; `showCart` is `rememberSaveable` | unverified. Cart/payment/tab are plain `MutableStateFlow`, no `SavedStateHandle`, so they are **lost** on process death (**Medium**). Crash: unverified. | Customer menu `remember` (`:460`) is transient, fine |
| Inventory | **fail (code):** `LazyColumn` over `filteredList` with no empty branch, so a blank list | **fail (code):** `searchQuery`, `selectedFilter`, dialog flags and 9 add-product fields plus 4 adjust-stock fields are plain `remember` (`:69-72`, `:372-380`, `:525-528`) | unverified, same loss | Typed product data is lost on rotation |
| Customers | **fail (code):** no empty branch (`:106-131`) | **fail (code):** dialog flags and form fields plain `remember` (`:50-51`, `:251-254`, `:330-332`) | unverified | Payment dialog pre-fills debt, which resets |
| Suppliers | **fail (code):** no empty branch (`:105`, `:126`) | **fail (code):** `:49-50`, `:241-245`, `:312` | unverified | |
| Expenses | **fail (code):** no empty branch (`:103`) | **fail (code):** `:51`, `:194-196` | unverified | |
| Staff | **fail (code):** no empty branch (`:107`) | **fail (code):** `:53`, `:245-248` (PIN field correctly not persisted) | unverified | |
| Sales History | **fail (code):** no empty branch (`:139`) | **fail (code):** `searchQuery` plain `remember` (`:58`) | unverified | |
| Analytics | unverified (no list; divide-by-zero on zero sales not checked) | pass (code): no user input state found | unverified | Needs a device or a JVM test with zero rows |
| Sync | pass (code): `:134` empty branch | pass (code) | unverified | Screen text says "Connected to Cloud Server" but the app is local (see Terms conflict, memory) |
| Login | n/a | **Username: pass (JVM, `LoginScreenTest.usernameSurvivesActivityRecreation`).** PIN deliberately not persisted | unverified | Fixed earlier this session |
| Terms | n/a | pass (code): `checked` and `splashDone` use `rememberSaveable` | unverified | |
| TopBar PIN dialog | n/a | fail-by-design (code): `pinInput` plain `remember` (`FasMovaPOSTopBar.kt:474`). Acceptable for a PIN. | unverified | Dialog flags (`:90`, `:322`) close on rotation (Low) |

Note on empty states: first-launch data is seeded, so the blank-list failures are reachable only after data is cleared or removed. Severity **Low-Medium** (no crash, no guidance).

## 8.4B Real-user resilience

| # | Check | Result | Notes |
|---|---|---|---|
| 1 | Empty first launch | pass (code) / device unverified | Demo data is seeded, so main actions render. POS has an empty block. The six list screens above would show blank if emptied. |
| 2 | **Double submit** | **fail (code) - High** | `checkoutSale()` (`FasMovaPOSViewModel.kt:326`) has no in-flight guard. `clearCart()` runs only after `repository.processSale` returns, so a second tap reads the same full cart and creates a **second sale and decrements stock twice** (different UUID and `TXN-` ref). The checkout button is only `enabled = cart.isNotEmpty() && paymentError == null` (`POSScreen.kt:436`). The `add*`/`adjustStock`/`recordCustomerPayment` functions also have no guard. Dialog forms probably dismiss on first tap, which would mitigate them: unverified. |
| 3 | Tap feedback / disabled / 48 dp | partial (code), unverified on device | Login and checkout have `enabled` states. In-flight disabled state is missing for every write (see 2). Target sizes unmeasured. |
| 4 | Raw error text | **fail (code) - Medium** | Two problems. (a) The VM has **no try/catch or exception handler** anywhere, and `processSale` uses `require(...)` (e.g. "Payment lines must add up to the sale total"). An exception inside `viewModelScope.launch` **crashes the app** rather than showing a message. (b) Invalid form input is silently ignored: `AddExpenseDialog` does nothing when amount/description is invalid (`ExpensesScreen.kt:241-246`), with no message. No stack trace or `undefined` text is rendered to the UI. |
| 5 | Keyboard covers field | **fail (code) - Medium** for Login; others unverified | No `imePadding` anywhere in the app. `LoginScreen` is a centred Box with no scroll, so on a small screen the keyboard can hide the PIN field and button. Dialogs use `Dialog`/`Surface` without scroll. |
| 6 | Dark mode | Supported (code); decision recorded: **support dark mode** | `values-night`, in-app toggle, `ThemeMode`. Open: 29 hardcoded `Color(0x..)`/`Color.White/Black` uses in screens and components need a dark-mode review (**Medium**, unverified), and the known light flash at startup before the saved theme loads (**Low**). |
| 7 | Back/background wipes form | **fail (code) - Medium** | Dialog forms are plain `remember`, so they are wiped on rotation, and the cart is lost on process death (see 8.4). PIN/payment details are correctly not persisted. |
| 8 | Offline behaviour | N/A | Fully local app (Room). The "offline" toggle is simulated. Retrofit/OkHttp/Firebase AI dependencies exist; no live network call path was exercised. |
| 9 | Slow-network button feedback | N/A | Same reason as 8. |

Triage order from the doc: 2 (High), then 4, 5, 7, 3.

## 8.5 Placeholder / dummy sweep

Command from the doc, scoped to `app/src/main/java` and `res`. No `REPLACE_ME`, `FIXME`, `localhost`, `10.0.2.2`, `test@` or `123456` hits.
- UI hint strings only: `PaymentPanel.kt:247` ("0712345678 or 254..."), `InventoryScreen.kt:177`, `POSScreen.kt:220`, `SalesHistoryScreen.kt:126` - intentional.
- `FasMovaPOSViewModel.kt:157` `id = "placeholder"` with empty `pinHash`: initial `currentUser` default, not a login credential. **Low**, intentional.
- `res/xml/data_extraction_rules.xml:8` template **TODO**. **Medium:** with `allowBackup="true"` and empty rules, the database (including PIN hashes and salts) is eligible for cloud backup/device transfer, which conflicts with Terms section 2 ("data stays on device").
- Seeded demo PINs (1234 / 2222 / 0000) from the earlier pass still stand. **High** before any real-user release.

## 8.6 ktlint source coverage

Pass. During this session's `codeQualityCheck` run, `runKtlintCheckOverMainSourceSet` and `runKtlintCheckOverTestSourceSet` executed and `ktlintTestSourceSetCheck` reported a real violation in `LoginScreenTest.kt`. That proves the plugin lints `.kt` sources, not only `*.kts` scripts (Lessons 8/16 not triggered).

## 8.7 Close-out

**Fixed after this pass (2026-10-08):** double-submit (8.4B #2, item 1 below). `checkoutSale` and every other data write (`add*`, `adjustStock`, `recordCustomerPayment`, `createPurchaseOrder`) now run through `SingleFlight`, so repeat taps while one is in flight are dropped. The Pay button is disabled and shows a spinner while checkout is saving. Covered by `SingleFlightTest` (JVM) and `CheckoutDoubleTapTest` (Robolectric, real ViewModel and Room: two back-to-back checkouts record one sale; verified it fails with the guard removed). Still unverified on a device.
**Fixed during the CI hardening pass:** Login username now `rememberSaveable`, covered by a JVM test.

**Known, documented, deferred (priority order):**
1. **High** - Double-submit on checkout and other writes (8.4B #2).
2. **High** - Room `exportSchema=false` + `fallbackToDestructiveMigration()` + untested `MIGRATION_1_2` (`FasMovaPOSDatabase.kt:46,47,70,94`): silent data loss on a bad upgrade.
3. **High** - Seeded demo PINs.
4. **Medium** - No exception handling in the ViewModel; failures crash (8.4B #4).
5. **Medium** - `allowBackup` with empty rules vs. Terms section 2 (8.5).
6. **Medium** - Form/dialog state lost on rotation across six screens (8.4, 8.4B #7); cart and tab lost on process death.
7. **Medium** - No IME handling on Login and forms (8.4B #5).
8. **Medium** - Launcher icon is an opaque JPG, monochrome layer is wrong (8.1/8.2).
9. **Medium (Gate 1/2)** - God ViewModel and preference reads in `MainActivity` (8.3).
10. **Low-Medium** - No empty states on six list screens; hero JPG 807 KB; dark-mode colour review; startup theme flash.

**Not verified (needs the emulator, awaiting your go under 7B):** rotation and process-death crashes, keyboard overlap on small screens, tap-target sizes, dark-mode rendering, the actual double-tap behaviour, launcher icon rendering.
