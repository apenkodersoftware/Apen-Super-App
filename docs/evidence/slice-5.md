# Slice5 — retail pricing/stock and restaurant correction candidate

13 selected JVM tests passed with zero failures, errors or skips. Debug and Android test APKs assembled successfully. Migration6->7 remains structurally validated; new6->7->8 migration preserves explicit quarter/half prices. Tier-pricing, discount/partial receipt and failed-transaction rollback tests pass. Mixed sold/unsold menu sorting and malformed comma-grouping regressions pass.

On emulator-5554 the schema8 upgrade was verified: all36 original data tables retain every original-column value exactly, including all earlier native trial records and the original R2 tables/note. New tier-price fields match retained product prices. IntegrityOK. Installed APK bytes match SHA256 9a033261eee01154468e7b7f5b5c273a503ccc6292071f65809df155ed0ed2ea (23,963,843bytes). This remains an intermediate debug review candidate.

| Scenario | Status | Tests | Screenshots | Not verified |
|---|---|---|---|---|
| SAT-D | Partial | Exact model and native steps1-5 pass | Six native screenshots in SAT-D | Second-emulator restore and final common summary |
| SAT-L | Partial | Exact model script; native fiscal assertions passed; correct reminder/chooser manually verified | Seven native screenshots in SAT-L | Fresh corrected full automation on schema8 running |
| SAT-R | Partial | Exact numeric script, menu editing/archive and sorting regression pass | First-attempt failure log retained | Fresh native scenario and four-tap benchmark on fixed schema8 running |
| SAT-S | Partial | Exact numeric script, tier/discount continuity and migration pass | Pending | Native part payment/collection/new stock script running |
| SAT-X | Partial | Isolation and two real migration/byte checks pass | Chooser plus dairy/rental native evidence | Whole second-device restore, PIN, SW/four homes, statement, final quality/release/accessibility |

No scenario is declared Complete from compilation or unit tests alone. The initial restaurant native attempt exposed a real mixed-number sorting crash; source is fixed with a regression test, and the native rerun must verify it.
