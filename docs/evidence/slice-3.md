# Slice 3 — landlord implementation and numeric checks

Eight selected tests passed, zero failures, errors or skips. SAT-L has the exact 28,000 expected, 13,000 received, 15,000 arrears, 10,500 collected less expenses, 31,000 posted owed after November preparation, and October-first allocation. Repeated catch-up creates no duplicates. A separate policy test covers mid-month move-in, missed months, move-out after the monthly charge, and held deposits excluded from income.

Native R2-to-R3 migration was verified on emulator-5554: all 17 pre-existing data tables are row-for-row identical, schema 7 integrity is OK, installed bytes match the dairy/retail candidate. Private rollback material is excluded from deliverables. Rental source compiles; the installed candidate does not yet enable rental.

| Scenario | Status | Tests | Screenshots | Not verified |
|---|---|---|---|---|
| SAT-D | Partial | Exact dairy numeric script passes | Chooser only; native dairy script being prepared | Native fiscal entry, second-emulator restore |
| SAT-L | Partial | Exact numeric script and rental policy test pass | Pending | Native unit/payment/arrears/share-sheet script |
| SAT-R | Partial | Numeric script passes with shared sale engine | Pending | Dedicated food UI and timed four-tap order |
| SAT-S | Partial | 280 sale, 100 received, 50 collection pass | Migrated retail screen captured as hierarchy | Native new workflow, stock polish |
| SAT-X | Partial | Isolation, migration, non-sales checks pass | Native chooser; only dairy/retail enabled in installed candidate | All four homes, SW, whole backup on second emulator, PIN recovery, common statement, full quality gates |

No scenario is marked Complete. Full definition of done requires the remaining native checks and restore.
