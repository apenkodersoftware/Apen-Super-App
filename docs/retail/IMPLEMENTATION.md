# MaliMali implementation scope

Started 23 October 2026 from FasMova POS commit `f9a3ad900cf44d066666423d59f49019ec8a8487`.

This is a separate source snapshot on branch `feature/malimali-foundation`, published to `apenkoder-software/malimali-App`. The original FasMova checkout and its untracked DianiFreshDemo folder are not part of the new app.

## First increment

- Owner/cashier shop app, one device, whole-item quantities.
- Separate Android application ID `com.apenkoder.malimali`, label MaliMali, development version 0.1.0. The inherited Kotlin namespace and class names remain to avoid unrelated structural changes.
- Owner creates their own shop and PIN. Training data stays opt-in and uses assorted household, kitchenware, cleaning, electrical and stationery products.
- Product categories come from the shop catalogue instead of a fixed list of unrelated industries.
- Sale, stock reduction, tenders, customer balances and outbox record are committed in one Room transaction.
- Checkout validates current stored stock and prices, combined quantities for repeated product lines, customer credit limits, payment totals and reused mobile-money references.
- Customer repayment receipt and balance update are committed together. Nonpositive/overlarge/replayed repayments are refused.
- Checkout totals use decimal arithmetic at two-decimal precision. The inherited database still stores monetary fields as Double; this is not a full integer-cent schema migration.
- Receipts and sales are local records. Mobile-money references are manually recorded. No backend or provider confirmation is configured.
- Removed the random-product scan button, fake cloud-sync behavior, fabricated report bars and 70%-of-revenue profit calculation. Profit is explicitly unavailable until actual per-sale costs exist.
- Tax is not configured. New sales record zero configured tax and omit the old fixed-rate tax line. This build does not generate fiscal/tax invoices.

## Verification approach

The inherited quality gate passed before implementation. The initial 12 checkout regression tests then ran on the inherited implementation: 9 failed and 3 passed. Failures included rollback after an injected SQLite trigger failure, stale stock/prices, credit limits, invalid quantities/tenders, reused payment references and excessive repayment.

Final verification results belong in the run report, not in historical README claims. Required checks for this increment are codeQualityCheck, testDebugUnitTest, lint and assembleDebug, each with its actual result. Test failure output must not be suppressed or converted to success.

## Deferred scope

- Partial returns/cancellations and their credit/refund behavior from IZO.
- Purchase-order line receiving and stock-movement history.
- Fractional quantities and carton/pack conversion rules.
- Actual cost snapshots and profit reporting; configured tax/fiscal integration.
- Automated payment-provider confirmation, cloud sync, actual barcode-camera scanning and printer integration.
- Full integer-cent storage migration and shop/role enforcement review.
- Device acceptance, accessibility/visual acceptance, release signing and distribution.

The inherited release signing fallback remains as source history/configuration. Only a development debug build is requested for this increment. Do not describe its APK as release-ready or suitable for real shop use before the deferred validation and requirements are addressed.

## Preserved files

CI/CD workflows, dependency versions, formatter/detekt configuration and baselines, Gradle wrapper, source schema history and signing logic are not changed by this increment. The build file changes only the new application's ID and version name. No private keystore properties, local.properties, .env file, caches, outputs or IDE state were copied from the source checkout.
