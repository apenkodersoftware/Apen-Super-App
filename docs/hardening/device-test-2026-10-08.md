# FasMova POS - on-device smoke test, 2026-10-08

Build: release APK `FasMovaPOS-1.0-7201e6b.apk`, signed with the release key.
Device: emulator-5554, Android 7.0 (API 24, the minimum supported), 720x1600.
Method: manual driving over `adb` (taps and screenshots). No boot polling, no builds while the emulator ran (Lesson 7B).
Findings only: nothing was changed during this pass.

## Passed on the device

| Area | Result |
|---|---|
| Install | Release APK installs. Installing over the old debug-signed build fails with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, as expected: uninstall the old build first. |
| First start | Terms, then a short loading spinner, then the setup wizard. |
| Setup wizard | Start is disabled until valid. `1234` is refused with "1234 is too easy to guess". PIN fields are masked. The keyboard does not hide any field. Sample-data option works. Business name appears in the top bar. |
| Sample data | Catalog, customers, suppliers and two staff loaded. |
| Cash sale | Cart, pay, receipt, stock reduced. |
| Double tap on Pay | Two taps in one `adb` command created exactly one sale. (The race is covered precisely by `CheckoutDoubleTapTest`; adb taps are too slow to hit the same millisecond.) |
| Mobile money form | Pay is disabled with "Enter the M-Pesa confirmation code from the customer's SMS" until a code is typed. The code is upper-cased as typed. |
| Forced PIN change | Signing in as the sample cashier with `1234` shows "Choose your own PIN" and nothing else. The Owner's PIN is refused ("Someone else already uses this PIN"). A private PIN is accepted and the app opens. |
| Cashier and backup | The cashier sees the Backup tab, but every action is disabled with "Only the Owner can back up or restore". |
| Login keyboard | With the keyboard open, both fields and Sign In stay visible. |
| Backup | Password dialog (6+ characters, must match), system file picker, 118 KB `.fmbk` written to Downloads, "Last backup" updated. About 25 s on this slow emulator. |
| Restore, wrong password | Clean message "Wrong password, or the file is not a valid backup." Data untouched. |
| Restore, correct password | Data reverted (stock 137 back to 138), the app restarted itself and the Owner session carried over. |
| Stability | No crash or ANR in the log for the whole session. |

## Findings

| # | Sev | Finding | Detail |
|---|---|---|---|
| 1 | **High** | Receipt prints a fabricated tax identity | Every receipt shows `FASMOVA POS RETAIL STORE`, `KRA PIN: P0512849302Z`, `TEL: +254 700 000111`, hardcoded, whatever the business is. A client must never issue receipts carrying a made-up KRA PIN. Fix: take the name from setup, make phone and KRA PIN optional fields (setup and settings), and print each only when entered. |
| 2 | **High** | Reused mobile-money code accepted | `QKJ9821LXM` already belonged to the sample sale, and a new M-Pesa sale with the same code was accepted. The duplicate check only looks at payment-line rows; the sample sale (and any sale made before the version-2 database) has its code only in the sale record. Fix: check the sale record's reference as well (exact match, including the "A + B" form used for split sales), plus a test with such a sale. |
| 3 | Medium | Misleading "Synced" badges | The top bar and every sale show a green "Synced". The app is local-only and never syncs; the Terms say so. Fix: remove the badges, or replace with "Saved on this device". |
| 4 | Medium | Terms contradict the app | Terms section 3 says "NO BACKUP" and tells users to rely on the device's own backup. The app now has its own encrypted backup. Needs rewriting, together with the lawyer review already pending. |
| 5 | Low-Med | Stale messages | The PIN screen keeps "Someone else already uses this PIN" after the user edits the PIN. The Backup screen keeps the previous result (for example the wrong-password message) visible while a new action runs. Clear them when the user edits or starts again. |
| 6 | Medium | Print and Share on the receipt do nothing | Both buttons are placeholders. Share could send the receipt text through WhatsApp/SMS with a standard share intent; Print needs a printer decision. Until then, hide them. |
| 7 | Low-Med | Slow on the oldest Android | On the API 24 emulator, first setup took roughly 15-20 s and a backup roughly 25 s, because key derivation on Android 7 falls back to SHA-1 with high iteration counts, with only a spinner or progress bar. Real phones are faster, but add a short explanation ("Securing your data...") and re-measure on a real low-end device. |
| 8 | Low | Backup tab icon | A cloud-upload icon suggests cloud storage; this backup is a file the user keeps. |

## Not covered

Rotation and process death of forms (known to lose input, see section 8 findings), dark-mode review of every screen, Kiswahili strings in situ, CSV export opened in a spreadsheet, small-phone and tablet layouts, a real device, and release-signed update-over-update installs.

## Next

Fix 1 and 2 first (compliance and payment integrity), then 3, 4, 5, 6. These need builds, so close or idle the emulator before building (Lesson 7).
