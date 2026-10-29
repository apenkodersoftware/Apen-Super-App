# MaliMali terms and language review - 25 October 2026

This follow-up completes the bilingual display and corrects the inherited FasMova terms for MaliMali. The owner requested broad translation coverage, reporting of long-word/widget limits, and explicit acceptance by existing users after this update.

The current terms revision is 20261025. The existing onboarding preference store and legacy acceptance flag are preserved. Acceptance requires both the legacy flag and this revision; an older acceptance therefore opens the corrected sheet. Only an explicit checkbox and Continue action records the new revision. The sheet supports English/Kiswahili selection. It can subsequently be read under More > Terms & privacy without changing acceptance.

Both documents identify MaliMali and retain eleven sections. App-specific descriptions distinguish the simple single-shopkeeper interface from retained complete-interface options, explain local records and manual backups, distinguish Deni from received money, and retain the provider/contact details. The existing age, warranty, liability and governing-law policy is retained. This is a content/implementation review, not a claim of regulatory certification.

Training product names are localized in Sell, Stock, Charge and receipts. Known categories, units, training customer/supplier names, order/payment/status labels and common expense categories have display translations. Search accepts the saved English or displayed Kiswahili product name. Exact ID/name checks preserve custom or renamed product records. No stored catalogue, receipt item name, amount, stock quantity or transaction method is rewritten.

Stock wording uses bidhaa instead of the misleading hisa. Credit uses deni consistently. Existing setup copy that incorrectly promised two training staff accounts was corrected. Customers, Suppliers and Expenses place their heading/total above a full-width action to accommodate longer labels. Full receipt item names can wrap rather than being cut to sixteen characters.

The added regression checks cover acceptance upgrade/restart, explicit acceptance controls, read-only review, bilingual resource/format parity, unchanged saved data, translated search/receipts, and native-rendered 360dp layouts with 1.5x text. Final test counts, emulator evidence, APK identity and layout findings are recorded in the accompanying dated verification report.

Long labels requiring attention include Bakuli la chuma lisilopata kutu, Kipande cha sabuni ya kufulia, Pakiti ya vibanio vya nguo, Vifaa vya kuandikia, and Namba ya bidhaa / SKU (hiari). Product cards and receipts wrap; category chips scroll horizontally. Technical identifiers and proper names (KES, PIN, SKU, CSV, LED, M-Pesa, Airtel Money, Google Drive, WhatsApp, shop names and transaction references) remain recognizable. Unknown custom descriptions remain as entered; they are not sent to an external translation service.

Database schemas/migrations, repositories, transaction/stock/price calculations, PIN authentication, app ID, signing, dependencies and cloud/CI configuration are unchanged. Only terms acceptance metadata is added to onboarding preferences, as requested.

Technical investigation: AndroidX FileStorage selects file movement by Android SDK. The acceptance tests use the same Robolectric SDK 36 environment as the project's Android tests so a plain JVM SDK-0 stub does not select a Windows-incompatible rename path; real files and assertions are retained. Reference: https://android.googlesource.com/platform/frameworks/support/+/4f98cdca5421e13b05f09fa3ea2df5704461b479/datastore/datastore-core/src/androidMain/kotlin/androidx/datastore/core/FileMoves.android.kt

Privacy-notice clarity reference: https://www.odpc.go.ke/data-protection-statement/

On-device follow-up: known English/Kiswahili categories now share one UI filter key, preserving their stored values. The expense form starts with the localized Rent label and recognizes Utilities, Taxes, Stock Freight and Miscellaneous display aliases. Verbose field captions are shortened in both languages. Customer, supplier, expense and payment/order forms use wider, scrollable dialogs with stacked actions so labels and the keyboard do not hide the final actions at larger text settings. Terms text and consent revision are unchanged by this follow-up.

The final 360dp/1.5x device check also exposed squeezed customer/supplier cards and a floating credit caption crossing the previous field. Customer/supplier details and amounts are stacked at full width in a single scrolling list. Forms reserve 24dp between fields so two-line captions can float without overlapping the previous field. Regression checks include real sample account cards and caption separation.
