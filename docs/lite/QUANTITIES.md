# FasMova Lite: required loose-goods sales

Mandatory fractional-sale requirements, implemented in the 0.1.0 debug review build. This document also records the original design constraints. Local build checks pass; the matching GitHub review release carries the final device-acceptance report and screenshots.

## Product decision

Fractional sales are mandatory in the first build. Whole-item-only delivery does not satisfy the owner's target small-shop workflow. Support piece counts, weight and volume while retaining a short selling flow and named-customer Deni.

## Shop-facing model

At product setup the owner chooses one selling basis:

- Pieces / Vipande: countable items or fixed packs, such as a loaf or sealed 1 kg packet.
- Weight / Uzito: loose sugar, rice, beans and solid cooking fat. The owner can enter stock and rates in kg while the app also accepts g.
- Volume / Ujazo: loose liquid cooking oil and similar goods. The owner can enter stock and rates in litres while the app also accepts ml.

Physical form does not determine price automatically. Solid cooking fat sold by weight is a weight product; bottled oil sold sealed is a piece product; loose oil sold by volume is a volume product. Do not infer a kg-to-litre conversion from the product name.

## Selling interaction

- Piece goods: tap to add one, then adjust the whole count.
- Weight or volume goods: tap to open a small quantity sheet showing the rate, quantity, amount and available stock.
- Weight presets: 1/4 kg, 1/2 kg and 1 kg, plus Custom. Presets are shortcuts and do not restrict custom quantities.
- Volume presets: 250 ml, 500 ml and 1 litre, plus Custom.
- Custom entry accepts a decimal with an explicit unit. Present 1.75 kg as an easy way to enter 1 3/4 kg. A fraction keypad may provide quarter/half/three-quarter shortcuts without requiring ambiguous text such as 1.3/4.
- Support a By amount / Kwa pesa quote for requests such as rice worth KES 30. Show the equivalent quantity, its precision and the final charge before adding it; do not silently round quantities while showing the entered amount as an exact conversion.
- An agreed amount different from the calculated rate must be stored explicitly as an override/discount, with its effect visible. The final quantity, amount and stock deduction must agree with the accepted quote.
- Add the quoted line to the normal cart. Payment and Deni do not require the owner to perform unit conversion.

## Exact units and money

Use an explicit measurement unit/dimension, an integer base quantity and an explicit base-unit scale. The first implementation retains bounded integer quantity columns for compatibility with the foundation, with a maximum of 2,147,483,647 base units per product. Parsing and adjustment limits reject overflow; combined quantities and stock arithmetic use Long. Decimal display and rate arithmetic use exact decimal calculations. Do not fix the current whole-item model by changing every quantity to binary Double.

Grams and millilitres cover the owner's stated examples. Final implementation must define the smallest supported increment for each dimension and reject unsupported precision visibly rather than truncate it. Money-based quotes may require a rounded measured quantity; show that result and reconcile the charge explicitly.

Examples at gram/ml precision:

| Customer request | Normalized quantity | Dimension |
| --- | ---: | --- |
| 1/4 kg sugar | 250 g | Weight |
| 1/2 kg rice | 500 g | Weight |
| 250 ml liquid cooking oil | 250 ml | Volume |
| 1/2 kg solid cooking fat | 500 g | Weight |
| 1 3/4 kg beans | 1750 g | Weight |

Store the price together with its pricing quantity, for example KES 160 per 1000 g. For a proportional rate, line amount = rate amount times sold base quantity divided by pricing base quantity. Compute a canonical rounded line amount once; the cart, saved receipt, tender totals, debt and reports must use that same amount. Canonical line amounts are rounded with BigDecimal at two decimal places before compatibility conversion to the inherited Double money columns. Receipts and reporting use those saved line/sale amounts. The quantity columns themselves contain no floating-point fractions.

Allow product-specific portion pricing where the owner charges a distinct price for a named quantity, such as 250 g. Selecting that portion must still deduct 250 g from the same stock pool. Custom-quantity quotes use the stated proportional rate unless an explicit portion/override is selected. Do not silently apply an inconsistent linear price.

## Stock and history

- Opening stock, additions, sales, wastage/removal and low-stock thresholds use the product's same dimension and base unit.
- Replenishing with a 50 kg sack adds 50000 g to the loose product. This is unit conversion, not a second stock balance for each fraction.
- Reject a combined sale quantity above current stored stock; retain atomic sale/payment/customer/stock updates and concurrency protection.
- Completed sale lines snapshot dimension, quantity, display/pricing units, rate and charged amount. Subsequent product-rate or unit edits must not rewrite a receipt or old report.
- Block changing a stocked product from weight to volume without an explicit supported conversion workflow. No assumed density conversion.
- Treat sealed packs and loose stock as separate products unless a deliberate pack-opening conversion is implemented. Do not deduct from both records automatically.
- Backup/export formats include quantity units, scale and version. Restore must preserve quantities and reject unsupported versions or ambiguous data.
- This is a new app identity with fresh business data. Schema history and any supported legacy import/restore still require explicit conversion rules. Never reinterpret an inherited quantity of 1 kg as 1 g; unknown unit mappings cannot be silently guessed.

## Required acceptance cases

1. A loose product starts with 10 kg. Sell 250 g, 500 g and 1.75 kg. The remaining stock is exactly 7.5 kg, including after restart and backup/restore.
2. At an illustrative KES 160/kg, the three quantities above charge KES 40, 80 and 280. Cart totals, persisted lines, receipt, tenders and reporting agree.
3. A 1.75 kg beans sale at KES 160/kg totals KES 280. Record KES 100 received and KES 180 Deni for a named customer. Later receive KES 50: outstanding debt becomes KES 130; original sales remain KES 280; receipts total KES 150.
4. A 20 litre loose-oil stock loses exactly 250 ml and then 500 ml, leaving 19.25 litres. Solid fat independently uses weight.
5. Repeated quarter quantities, custom amounts, a non-terminating money-to-weight conversion and the smallest allowed increments follow the documented rounding/precision rules. No hidden truncation or mismatched charge.
6. Repeated cart lines and simultaneous attempts to sell the last remaining weight/volume cannot oversell. Invalid, zero, negative, overflow and wrong-dimension entries are refused with readable messages.
7. A deliberate portion price differs from the proportional price; the displayed quote and saved receipt retain the chosen price while deducting the correct physical quantity.
8. English/Kiswahili quantity entry, keyboard, long labels, receipts and large text remain readable. Piece products continue to behave as whole counts.
9. Price changes after checkout leave prior receipts and debt unchanged. Stock-unit edits and unsupported imports cannot reinterpret historical quantities.

## Source impact observed

The current publication source has integer `ProductEntity.stockQuantity`, integer `SaleItemEntity.quantity`, integer `CartItem.quantity` and integer DAO stock changes. Quantity use was found across 13 production files, including both POS interfaces, stock forms, the ViewModel, receipt display/sharing and checkout orchestration. Backup/database schemas, exports and relevant tests also need review. The work must therefore extend through the transaction pipeline rather than stop at a decimal input field.
