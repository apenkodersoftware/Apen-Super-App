# FasMova Lite: source assessment and proposed first delivery

Historical feasibility assessment from 26 October 2026, written before implementation. A separate FasMova Lite application has since been built and passed local quality, unit and lint checks. See README.md and the matching GitHub review release for current results. Candidate comparisons below describe the initial inspection.

## Recommendation

Create FasMova Lite as a separate application from the current MaliMali publication source snapshot. That source already adapts FasMova POS and contains the simplified workflow, transaction hardening, English/Kiswahili resources and layout corrections completed for MaliMali. Reusing it avoids repeating those changes on the original FasMova checkout.

Keep the first delivery to a single shop on one device. Fractional weight and volume sales are mandatory in the first build, alongside piece and fixed-pack sales. The owner explicitly confirmed that loose quantities are fundamental to the target shops. See `QUANTITIES.md` for the revised quantity model and acceptance cases.

## Local evidence

| Candidate | Current evidence | Assessment |
| --- | --- | --- |
| MaliMali publication source | `C:\Users\ApenkoderSoftware\Documents\Codex\2026-10-23\you\work\malimali-publish`, clean working tree, HEAD `8e32bb363e3dbeb1a880e54d15fe328ee32d439d` | Preferred starting snapshot. Default-on simple interface and checkout regression coverage already exist. |
| Original FasMova POS | `C:\Android Projects\FasMovaPOS`, branch `feature/onboarding-payments-darkmode-swahili`, HEAD `f9a3ad900cf44d066666423d59f49019ec8a8487`; untracked `DianiFreshDemo/` | Broader interface; random-product scan and local-only sync marking still present. More work to repeat the later corrections. |
| Mama Ntilie | R7 business-summary source, final emulator screenshot and 26 October verification report inspected after the owner's reporting request | Use its reporting presentation, period controls and treatment of receipts versus credit as the first-version reporting reference. Keep the MaliMali retail foundation. |

MaliMali's `docs/retail/IMPLEMENTATION.md` records the original FasMova source commit and separate snapshot provenance. That FasMova commit object is not available in the publication repository, so ancestry was not independently established using Git merge-base.

The saved checkpoint at `C:\Users\ApenkoderSoftware\Documents\Codex\2026-10-23\you\outputs\MaliMali-language-terms-20261025\verification\complete\result.json` records 122 passing tests across 30 suites, zero failures/errors/skips, zero lint errors and 115 warnings on 25 October. Today's hash comparison matched all 160 checkpoint source files. Tests and emulator acceptance were not rerun today; those prior results do not certify a future FasMova Lite build.

## Proposed first workflow

| Main tab | Shopkeeper task |
| --- | --- |
| Sell / Uza | Search goods; choose pieces, weight, volume or a money-based loose-goods quote; charge; record Cash/M-Pesa/Deni; view a receipt. M-Pesa remains a manually recorded payment. |
| Stock / Bidhaa | Add goods with a sale basis (pieces, weight or volume), rate and opening stock; replenish the same stock balance and see low-stock warnings. Keep optional details secondary. |
| Today / Leo | Compact business summary using the Mama Ntilie R7 presentation, with recorded sales, expenses and a clearly qualified estimated result; show received payments and credit separately. Link to the full business report. |
| More / Zaidi | Reports / Business summary, customer debts and repayments, expenses, backup/restore, language, shop settings and help. Defer supplier/order access from the first Lite interface. |

Example: create the shop and owner PIN; add bottled water, loose sugar, rice, liquid oil, solid cooking fat and beans. Sell pieces and fractional quantities; record a part payment with the balance on a named customer's Deni; collect a later partial repayment; verify stock, reports, restart persistence and restored backups.

## Reporting reference added on 26 October

The owner explicitly requested the business reporting and presentation from Mama Ntilie R7 as a reference for FasMova Lite. This is part of the first-version scope, not an advanced analytics add-on.

Inspected current sources:

- `C:\Android Projects\DianiQuickSale\app\src\main\java\com\nommolsoftware\dianiquicksale\BusinessSummaryState.kt`
- `C:\Android Projects\DianiQuickSale\app\src\main\java\com\nommolsoftware\dianiquicksale\BusinessSummaryScreen.kt`
- `C:\Users\ApenkoderSoftware\Documents\Codex\2026-10-23\p\outputs\mama-business-summary-review-2026-10-26.md`
- `C:\Users\ApenkoderSoftware\Documents\Codex\2026-10-23\p\outputs\mama-business-summary-evidence-2026-10-26\c30-final-dark.png`

Carry over these behaviours and presentation choices:

- Large, readable labelled amounts, grouped panels, a highlighted estimate and clear loss treatment.
- Today by default, with Daily / Weekly / Monthly / Yearly choices and the actual selected dates visible.
- Automatic updates after a saved sale, expense or debt collection. Sales and expenses use the same calendar window.
- Credit sales contribute to sales; unpaid credit is excluded from payments received.
- Repayment of an older debt contributes to receipts on its repayment date without becoming a second sale.
- A clearly labelled received-payments-minus-recorded-expenses figure, distinct from an exact till or mobile-wallet balance.
- A prominent Add expense action, bilingual labels and readable light/dark layouts.

Adapt the report to Lite's Room-backed sales, sale-payment, expense and customer-payment records. Mama R7's records remain in memory according to its review report; that storage behaviour is not the reuse target. Report totals must survive a Lite restart.

The Mama estimate is recorded sales after discounts minus recorded expenses. Lite may expose the same basic calculation with an explicit estimate label and explanation. Retail profit needs separate handling of stock costs: current product cost alone does not establish the historical cost of sold goods, and replenishment must not be deducted twice. Keep this limitation visible until the per-sale cost and expense treatment is implemented and verified. No exact wallet balance or verified accounting profit is promised by this scope.

## Adaptation work

1. Create a separate source checkout and application ID, proposed `com.apenkoder.fasmovalite`, so it can coexist with MaliMali and Mama Ntilie. Keep inherited internal Kotlin names where changing them adds no customer benefit.
2. Update launcher identity, icon, onboarding, receipts, terms/help wording, backup/export names and tests affected by identity. Review backup identification so sibling-app files cannot be silently treated as Lite backups.
3. Replace MaliMali-specific training products with optional general-shop examples. Normal setup starts with an empty catalogue and no inherited business records or consent.
4. Implement and verify the base-unit quantity model through storage, stock, cart, checkout, receipts, exports and backup/restore. Preserve explicit unit/rate snapshots on completed sale lines. Fractional sales must work before the first review APK is considered complete.
5. Keep the simple interface; remove unnecessary entry points before considering deletion of shared database tables or repositories. Avoid schema churn solely to hide advanced features.
6. Adapt the Mama R7 business-summary presentation and date/receipt rules to Lite's saved retail records. Verify losses, discounts, split tenders where enabled, credit, repayment without double-counting, period boundaries and updates after restart.
7. Run code quality, unit tests, lint and APK assembly. On the existing emulator, verify fresh setup, piece/weight/volume sales and stock deduction, restart persistence, credit and repayment, backup/restore, English/Kiswahili, keyboard and large-text layouts. Use separate Lite data for these checks.
8. Deliver a debug review APK, verify its package/signature and SHA-256, and match the installed APK bytes. Owner review follows; release signing and production distribution are separate milestones.

## Scope limits that affect delivery

- The inherited quantities are whole-item integers; this must change for Lite's required weight/volume support. Unit-aware base quantities, rate snapshots, checked arithmetic and storage migration are substantive first-build work. General automatic carton-to-piece conversion remains separate from the required kg/g and litre/ml conversion.
- Supplier order receiving, returns/cancellations and a stock-movement ledger are not established as complete in this base. Do not include them in the first-delivery promise.
- No automated M-Pesa verification, cloud synchronization, camera barcode scan, printer integration, verified profit calculation or fiscal integration is promised.
- The source assessment supports a smaller adaptation than starting again from original FasMova. It does not establish a calendar completion date or the current build environment's readiness.

## Source pointers

Paths below are relative to the MaliMali publication checkout unless stated otherwise.

- `docs/retail/IMPLEMENTATION.md`: foundation provenance, hardening and deferred scope.
- `app/build.gradle.kts`: current application ID and version.
- `app/src/main/java/com/aistudio/fasmovapos/app/ui/simple/SimpleMode.kt`: simple interface enabled by default.
- `app/src/main/java/com/aistudio/fasmovapos/app/ui/simple/SimpleNavigation.kt`: four tabs and secondary destinations.
- `app/src/main/java/com/aistudio/fasmovapos/app/ui/simple/SimpleStockScreen.kt`: integer quantity forms and optional product details.
- `app/src/main/java/com/aistudio/fasmovapos/app/ui/simple/SimpleChargeScreen.kt`: simple payment methods.
- `app/src/main/java/com/aistudio/fasmovapos/app/data/db/entities/Entities.kt`: integer stock and sale quantities.
- `app/src/main/java/com/aistudio/fasmovapos/app/data/repository/SampleData.kt`: current optional household-goods training catalogue.
- `app/src/test/java/com/aistudio/fasmovapos/app/data/repository/RetailCheckoutTest.kt`: checkout rollback, stale stock/price, credit, payment reference, repayment and concurrency regression cases.
