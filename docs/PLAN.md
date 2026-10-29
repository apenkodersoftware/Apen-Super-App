# Apen Super App implementation plan

28 October 2026, Africa/Nairobi. Authority: the user-supplied build brief. All four livelihoods and SAT-X are the delivery scope. A passing retail baseline alone is not completion.

## Baseline and preservation

Isolated source: `work/apen-super`, extracted from the verified R2 source archive (SHA-256 `6cd7305d4d0302e3fd99e297790c917da0400f03f3fc712d0c6762d9dc3981e2`). R2 source and deliverables remain untouched; baseline file hashes are recorded outside this tree. There is no Git metadata in the source archive. No commit, publication or remote write is part of this request. Package identity stays `com.apenkoder.apen.review` for migration testing; this will be review version R3. A release-variant build using the existing public debug fallback is not production signed.

## Inspection and reuse

- R2: Kotlin/Compose/Room schema 6; local PIN authentication and recovery; encrypted whole-database backup; personal notes and Inuka facts; PDF/CSV export; quantities in thousandths; unit/regression/Room migration tests. Reuse these mechanisms and review their new boundaries.
- Mama Ntilie, available as DianiQuickSale: inspect `MamaReviewCatalog.kt` and menu/cart flow in `MainActivity.kt`. Reuse interaction ideas (large menu choices and immediate tender) and category vocabulary. Its in-memory transaction store is not a persistence donor.
- Dwelify: inspect `ui/theme/Color.kt` and `ui/screens/OnboardingScreen.kt` for warmth, strong identity and legible hero treatment. No marketplace, backend, listing or account features, photos or copied artwork.
- Kona Shop and Kazi Mob: no folders with those names found in the Android Projects inventory. Existing retail donors and Kipato-style accounting boundaries cover the relevant references; no fictional source-reuse claim.
- No applicable AGENTS.md found in this source or its ancestors. Sibling checkouts are read-only references.

## Foundation decisions

Use the existing Room database, increment schema to 7, and add business-scoped domain tables. Every new business record has a stable ID, business ID, activity date, original recorded time (nullable for legacy data), schema version and void flag. Business profiles identify RETAIL, RESTAURANT, DAIRY or RENTAL, KES and a permanent demo flag. Staff/PIN and personal loan notes belong to the installation; loan notes retain their existing distinct meaning and are included in the same backup.

Use actual domain source tables for milk deliveries; rental properties, units, tenancies and monthly charges; commerce items, sales and lines; receipts, deductions, expenses and non-sales money. Charges and receipts link to one party account. A shared ledger is a transactional projection of source records, with one stable posting key per source. Never enter a collection as another sale. Cash received excludes deductions. A cost deduction creates an expense, while shares/advance or loan recovery settle the buyer balance without creating expense. Deposits are held money, not rent or sales.

Migration copies existing retail data into the scoped domain model once, keeping the original tables and R2 history intact. Map old stable IDs, actual transaction dates and original recorded times; use null where entry time is unknown. Preserve legacy quantities, costs, references, staff and personal notes. Old tables become a retained compatibility archive; new business screens use the unified model as their source. Prevent rerunning the import on every app open. A restored schema-6 backup follows the same migration.

All domain writes, ledger postings, account allocation and history snapshots commit together. Edits and voids require a reason and preserve before/after values. Reject cross-business references, negative/overflow amounts, invalid dates and duplicate rent charges. A receipt settles the oldest outstanding charges first; rental allocation is visible. Do not allow a receipt to be counted twice.

Keep local authentication; “no accounts” means no cloud registration. Use the existing username/PIN recovery path. Do not add networking, analytics or payment SDKs. Existing official-site actions remain explicit user choices.

## SAT traceability

| Step | Screen / action | Implementation | Test / evidence |
|---|---|---|---|
| D1 | Choose Dairy; Buyers > Add buyer | SuperFirstRun, BusinessProfile, Party | SuperAcceptanceTest.satD; SAT-D/01-home,02-buyer |
| D2 | Milk > Morning/Evening with date and buyer | DairyHome, MilkEditor, MilkDelivery, DairyRepository | exact 42 L / KES 2,100 / owed 2,100; SAT-D/03-deliveries |
| D3 | Buyers > Record payment plus Feed deduction | PartyAccountScreen, ReceiptEditor, Receipt/Deduction/Expense and ledger transaction | received 1,800, cost 300, owed 0; SAT-D/04-settlement |
| D4 | Costs > Vet and AI | CostsScreen, Expense | spent 800, sales less expenses 1,300; SAT-D/05-costs |
| D5 | Delivery > Edit with reason; History | MilkEditor, EventHistory and rebuilt posting | 43 L / 2,150 / owed 50 / difference 1,350; before/after/reason; SAT-D/06-correction |
| D6 | More > Backup; second emulator Restore | existing BackupManager plus schema-7 data | encrypted round trip and second-device equality; SAT-D/07-restore |
| L1 | Units > Property and three occupied units | RentalHome, UnitEditor, RentalProperty/RentalUnit/Tenancy | October expected 28,000; SAT-L/01-units |
| L2 | Rent row > Payment | ReceiptEditor and oldest charge allocations | received 13,000 / arrears 15,000; SAT-L/02-payments |
| L3 | Costs > Repairs | CostsScreen, Expense | collected less expenses 10,500; SAT-L/03-repair |
| L4 | B1 > Move out 1 Nov; open Nov | UnitEditor, RentChargeGenerator | November 2 charges only, owed 31,000, repeat-open idempotent; SAT-L/04-november |
| L5 | A2 > Payment 3,000 | Receipt allocation transaction | October cleared first; SAT-L/05-allocation |
| L6 | A2 > Reminder | ReminderPreview, Android ACTION_SEND chooser | A2 and correct remaining amount; cancel without sending; SAT-L/06-reminder |
| R1 | Choose Food; Menu > Add five items | RestaurantHome, MenuEditor, CommerceItem | no stock numbers; SAT-R/01-menu |
| R2 | Sell menu quantities | RestaurantCart, CommerceSale/Line | chai 25 + chapati 15 + githeri 20, sales 2,000; SAT-R/02-sales |
| R3 | Deni to regular | PartyAccountScreen, commerce charge/receipt | received 1,880 / owed 120; SAT-R/03-deni |
| R4 | Shopping today > 900 | CostsScreen with restaurant categories | difference 1,100; SAT-R/04-shopping |
| R5 | Next-day collection | ReceiptEditor | sales unchanged, received 2,000 / owed 0; SAT-R/05-collection |
| R6 | Three menu taps + Cash | RestaurantHome direct tender | <=4 taps and measured <10 seconds; SAT-R/06-speed |
| S1 | Sell 280, part payment 100 | RetailHome/checkout, existing quantity conventions | sales 280 / owed 180; SAT-S/01-part-payment |
| S2 | Deni tab > collect 50 | shared party account | owed 130 / received 150 / sales unchanged; SAT-S/02-collection |
| S3 | Today and stock polish | RetailHome, StockScreen, Summary | no profit claim; left labels, visible restock; SAT-S/03-stock |
| X1 | First-run illustrated type chooser, four homes | SuperFirstRun, LivelihoodTheme/Illustration, per-type tabs | four visually distinct native images, active home matches type |
| X2 | Add second business; header switch | BusinessSwitcher, business-scoped queries/validators | dairy + retail isolation, no cross-business party/item writes |
| X3 | Whole backup and second-device restore | BackupManager, schema 7 migration and history | all profiles, accounts, edits, notes and PIN survive; actual second device required |
| X4 | EN/SW on four homes | Android resource pairs, SuperFormatting | resource parity plus native screenshots; finance glossary in swahili-review.md |
| X5 | PIN and backup recovery | existing local auth, RecoveryDialog | actual sign-out, recovery, reset PIN and sign-in; original review state retained |
| X6 | Chosen-period statement for every type | shared SuperStatement from transaction snapshot; existing BusinessStatement PDF mechanism | privacy-default preview, CSV schema/version, as-of-now owed, report fingerprint, local save; no recipient selected |

## Slice order and reporting

0. Inspect, this plan, full numeric scripted tests first as an expected failing run. Preserve the red receipt. Do not treat compile failure as scenario evidence.
1. Room foundation, migration, account/ledger integrity, provenance, design tokens, first-run and per-type shell. SAT-S numeric accounting must work before building missing livelihoods.
2. Dairy: complete SAT-D including deductions and visible correction history.
3. Rental: complete SAT-L including missed months, dated vacancy and allocation.
4. Restaurant: complete SAT-R including four-tap tender, custom amount and no stock model.
5. Retail polish: Deni promoted, visible restock, stock labels and distinct design.
6. Cross-cutting: whole backup/second device, history, PIN, SW, then statements.
7. Hardening: gates, small-screen/130% checks, contrast and target sizes, low-RAM emulator, APK size/startup, debug and release variants. Physical/outdoor results require actual hardware and must remain unverified if none is available.

After each slice, persist `docs/evidence/slice-N.md` and report Scenario / Status (Complete, Partial, Not started) / Tests / Screenshots / Not verified. No scenario is Complete until its full exact script and required screenshots exist. A numerical model test alone does not prove native entry ergonomics or second-emulator restore. All output reports carry the actual APK identity tested.

## Assumptions and risks

- Test date anchors: Dairy day 1 = 27 Oct and day 2 = 28 Oct 2026; rental October/November are explicit selected months. Do not alter the emulator clock to fake future charges. Opening a selected month creates due occupied-unit charges through that month and explains what was created; ordinary app open catches up to the current month.
- A month's rent is due in full for a tenancy active at the configured due date. No prorating is implied. Move-out before November stops November charges but keeps prior arrears. Record this policy in the unit form.
- Rent receipts apply oldest first. “Collected of expected” shows receipts allocated to that selected month's charges, with other collected money separately in details; business summary received uses receipt activity dates. No deposit is income.
- Dairy earnings use integer thousandths of a litre and integer cents, with explicit half-up rounding. Cost deductions settle debt but reduce cash received; ledger settlement is not cash.
- Restaurant normal cash orders have immediate tender buttons; M-Pesa references are optional manually recorded references and no payment confirmation claim. Credit or part-paid orders must choose a party.
- Money-in categories include owner capital, loan proceeds, grants and transfers. They are excluded from sales and separately labelled. No aggregate cash-balance claim.
- Existing long-lived R2 data remain recoverable. A compatible older backup migrates forward; R2 cannot open a schema-7 backup. Do not distribute R3 to customers as production without an approved signing/update plan.
- No emulator is currently attached to ADB at inspection time; one configured AVD was found. Native and second-emulator checks remain pending until actually run. Do independent implementation work first.
- No production signing key has been supplied. Build the release variant to verify compilation; retain truthful debug-signing metadata. Never create an invented production identity or publish.
- Genuine physical-phone, outdoor and native-language review cannot be manufactured by tests. List those limits explicitly rather than calling them green.

### Slice 3 rental policy clarification

Monthly charges are created at the month beginning for occupied units, with missed months caught up idempotently. Mid-month move-in receives a full first-month charge on move-in, with first due date no earlier than move-in; no automatic prorating. Move-out after the first retains an already-incurred monthly charge; vacancy from the first stops that month. Vacancy is displayed as of today for the current month, month end for past months, and month beginning for future months. Deposits are held money, never sales or received trade income. This replaces the earlier due-date occupancy assumption.
# Updated device scope — 29 October 2026

Only emulator-5554 is permitted, by the user's latest explicit instruction. Its configured customer-phone geometry is retained. The original second-emulator restore steps are superseded by safe same-device recovery validation, with current records preserved. Do not start/use5556 or any other emulator, and do not claim second-device recovery. See TEST-DEVICE-POLICY.md. The completed backup was authenticated and native same-device recovery subsequently passed; all 36 data tables were compared. See evidence/SAT-X/same-device-recovery-verification.json. No second-device claim is made.


## Current iteration boundary - 29 October 2026

The current schema is 8. Dairy, Rental, Restaurant and Retail are the four implemented livelihoods. Shared statements and final regression/device gates have passed for local review on emulator-5554. See R3-HANDOVER.md for exact scope, identities, evidence and customer-release limits. Poultry is explicitly deferred until this iteration passes its review gates, following the user's confirmation on 29 October. See POULTRY-NEXT-ITERATION.md in the source documentation.


## Final R3 local review status - 29 October 2026

All four exact native transaction scripts, shared statement exports, same-device recovery, history, larger-text checks and the 1,000-record fixture passed. The main APK was installed and read-back verified; all 36 data tables and the original PIN were unchanged. The temporary verification app was removed. Production/customer approval, physical/outdoor and human language review remain separate pending gates. Poultry remains the next planned scenario.
