# Apen Super App R3 - review handover

29 October 2026. The four livelihoods and shared features have passed the local review gates listed below. This is an owner-review debug APK, not an approved customer release. Poultry is deferred to the next iteration.

## Which app to open

Open **Apen Super R3 Review** on **emulator-5554**. It is package `com.apenkoder.apen.review`, version `0.3.0-super-review-r3` (code 3), database schema 8. The temporary **Apen R3 Verification** app and both test drivers have been removed. Only the main Apen package remains. The app is left on the Demo Dairy home in English. Use the business-name dropdown to inspect Rental, Restaurant and Retail; EN/SW changes language.

The emulator remains 720 x 1600 at density 240. The 130% font test restored its original font scale of 1.0. No other emulator was used in these final checks. The main app's data were not cleared or replaced. All values in 36 data tables, including the original PIN, match the authenticated pre-final backup.

## Acceptance matrix

“Complete for local review” means the exact numerical script and native screenshots passed within the authorized emulator scope. The physical-phone and human review limits below apply to every scenario.

| Scenario | Status | Tests and expected results | Screenshots | Not verified |
|---|---|---|---|---|
| Dairy | Complete for local review | 42 L / KES 2,100; KES 1,800 payment plus KES 300 feed deduction; KES 500 vet cost; reasoned correction to 43 L / KES 2,150, owed KES 50, recorded difference KES 1,350. Native and JVM acceptance passed. | SAT-D plus EN/SW homes and 130% previews in SAT-X | Physical farm use, outdoor readability, independent language review |
| Landlord | Complete for local review | October charges KES 28,000; receipts KES 13,000; repair KES 2,500; difference KES 10,500. November vacancy retains old arrears, creates no duplicate charge; KES 3,000 payment allocates oldest first. Reminder chooser opened and cancelled without sending. | SAT-L plus EN/SW homes and 130% previews | Physical landlord workflow and recipient delivery |
| Restaurant | Complete for local review | KES 2,000 sales, KES 1,880 received, KES 120 Deni; shopping KES 900; later collection does not create another sale. Three-item cash order: four native actions, 1,005 ms including persistence, no keyboard actions. | SAT-R plus EN/SW homes and 130% previews | Physical counter speed, actual payment confirmation |
| Retail | Complete for local review | KES 280 sale, KES 100 received, KES 180 owed; later KES 50 collection gives received KES 150 and owed KES 130 while sales stay KES 280; stock left 9. | SAT-S plus EN/SW homes and 130% previews | Physical shop operation, historical cost-of-goods profit |
| Shared features | Complete for authorized local review | All four fresh starts; business separation; reasoned edit/void history; encrypted native same-device recovery and 36-table comparison; recovered PIN login; all four statement preview/consent/PDF/CSV/chooser flows; 1,000-record restart and history scrolling; final main-app homes and data preservation. | SAT-X and hardening, including final-primary-handoff.png | Second physical device, independent native Kiswahili review, production distribution approval |
| Poultry | Planned, not implemented in R3 | Separate flock/egg/bird-sales scenario plan provided | Separate roadmap | All Poultry implementation and acceptance |

## Shared reporting and the final fix

Every type exports the same versioned 41-field CSV format (`apen-business-statement/2`) and a matching PDF. Each report freezes the selected business/period, totals, coverage, definitions, generation time, report ID and app/schema version. Individual customer, buyer and tenant names, phone numbers, payment references and personal loan notes are omitted. The owner previews the content and explicitly consents before save/share actions enable. Opening the Android share chooser does not send anything automatically.

The final lifecycle correction keeps the same statement and consent state when Android resumes the app after a file picker or share chooser. All four types passed actual PDF and CSV saving and chooser cancellation on the corrected runtime. The eight saved files were pulled back and checked: exact schema, native preview values, independent SQLite ledger and coverage calculations, matching report IDs and CSV digests in PDFs, A4 page margins and readable rendered pages. Every page of all four PDFs was visually reviewed.

At 130% text size, all four homes and statement previews were exercised in English and Kiswahili and 24 screenshots reviewed. Long navigation labels wrap onto additional lines. Statement content is scrollable; the screenshot at a scroll boundary is not the whole statement. The original font scale and emulator dimensions were verified restored.

## Gates, identity and performance

- 188 JVM tests across 52 suites: zero failures, errors or skips.
- ktlint, detekt and codeQualityCheck passed; configuration and baselines were not weakened.
- Android lint: zero errors, 98 warnings. R2 had 97 warnings; the additional warning recommends a preference-edit KTX style change. Warnings are not described as zero.
- Debug, release-variant and Android test builds passed. The release variant uses the public debug signing fallback; it is not a production-signed release.
- 332 paired English/Kiswahili labels and 24 explicit design-token text/surface contrast pairs checked at 4.5:1 or better. These checks do not replace outdoor or human language review.
- A disposable 1,000-delivery fixture created through validated domain APIs retained all records after activity restart and history scrolled to index 900. Instrumented launch-to-ready was 8,116 ms and process PSS 67,946 KB. This is an emulator measurement, not physical-phone performance.
- The final main app's force-stop launch activity times were 2,915 / 2,834 / 2,958 ms; host command durations 4,202 / 5,057 / 4,161 ms. The first Android TotalTime reported 14,543 ms despite WaitTime 2,966 ms; the raw result is preserved rather than represented as a clean aggregate startup measure. Main-app PSS snapshot: 43,632 KB. These timings do not measure business-data-ready time.
- The crash buffer contained no entries during the final 20:xx/21:xx acceptance window. Older system crash entries were retained and not cleared.

Final APK: **23,331,425 bytes**. SHA-256:

`fb5827f14160d54b7f005b20cc42d0e9b67ee9291554db44987ce19f18f33cf3`

The installed APK was read back and matches these exact bytes. Public Android debug signer SHA-256:

`a6386dbf11c311d6d1b4dc383ff59523369f1b87344726cc2c2836b40f0ffd00`

Runtime source fingerprint (180 main-source files):

`c61fbef0b9199abb429e77ad7cea553f167b711afff160ba7ae83d81444e4ace`

## What the app helps the customer do

Apen gives dairy farmers, small landlords, food businesses and shopkeepers one offline app with separate business records and appropriate day-to-day screens. It helps them follow unpaid amounts, compare sales or milk value with collections, record costs, follow stock where relevant, and prepare an understandable statement for their own decisions or a discussion with Inuka. The product summary describes these capabilities and practical benefits in plain language.

Earned/sales/rent charges, received money, deductions, expenses, held deposits and other money-in remain distinct. A later debt collection is not another sale. Sales less recorded expenses is not full profit or a wallet/cash balance. Statements disclose missing days and unknown original entry times. Owed is the balance of all currently posted records, including any future-dated saved entries, rather than a historical period-end balance.

## Customer-release work still requiring real-world evidence

The local review gate is green. This does not approve Monday's customer rollout. Before distribution, complete owner/customer acceptance on the intended physical phone, bright-outdoor and human Kiswahili review, and agree the production signing/update/distribution plan. Second-device recovery was not tested because only emulator-5554 was authorized; native recovery used a separate disposable installation on that same device and compared all data tables. No real payment, recipient delivery, Inuka backend integration or loan approval was verified or implied.

The application does not verify M-Pesa transactions, provide cloud synchronisation, submit loan applications, score creditworthiness, promise finance or prove that self-entered records are true. The next Poultry scenario has its own roadmap and acceptance plan; it is not hidden inside this R3 scope.

## Files and reproducibility

- `Apen-Super-R3-0.3.0-review-20261029-debug.apk`: the exact main APK installed and verified.
- `Apen-Super-R3-Source.zip`: current source and documentation. Build/cache output, local machine settings, databases, backups and private keys/secrets are excluded. Signing keys are omitted from the public source archive; see PUBLICATION.md to generate a local review key.
- `Apen-Super-R3-Evidence.zip`: native screenshots, passing logs, example PDF/CSV exports and verification receipts under `docs/evidence/`. Extract alongside the source if needed.
- `Apen-Super-R3-Artifact-Manifest.json`, `Apen-Super-R3-SHA256.txt`, `Apen-Super-R3-Signature.txt`: identity and integrity records.
- `Apen-Super-R3-Product-Summary.md`: audience, capabilities, objectives and benefits.
- `Apen-Poultry-Scenario-Plan.md`: deferred next scenario.

Source work remains local and uncommitted. Nothing was published or deployed. The R2 source baseline was preserved and checked separately. Earlier attempts and private recovery working files remain in the local work area and are excluded from deliverables.


## Publication update - 30 October 2026

This handover records the completed 29 October local review. Source and associated review artifacts are now being published to apenkodersoftware/Apen-Super-App under the user's explicit instruction. The public source omits signing keys and adds CI generation of an ephemeral debug key. The original tested APK is unchanged. See PUBLICATION.md and the publication manifest for public archive identities.
