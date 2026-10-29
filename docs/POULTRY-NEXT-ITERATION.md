# Apen Poultry: fifth livelihood scenario

29 October 2026 · proposed next iteration

## Delivery decision

Complete the current Dairy, Landlord, Restaurant, Retail and Shared acceptance work first. Add Poultry as the fifth livelihood in the following iteration, with its own operating screens and acceptance evidence. Poultry is not implemented or included in the current APK scope. Do not make a Poultry card selectable until its workflow works.

All Android validation must use **emulator-5554 only**, retaining its customer-phone geometry. Recovery testing uses an isolated verification installation on that same emulator, preserving the primary app's records and PIN. No second-emulator claim is permitted.

## Who it serves

Small poultry owners keeping layers, broilers or a mixed operation. One poultry business can contain separate flocks or batches, so egg production and a batch of birds for sale remain distinguishable. A farmer can also keep a separate Dairy or Retail business in the same Apen installation.

## Daily workflow

The poultry home has a flock illustration, warm colours and quick actions for Record eggs, Sell eggs and Record bird movement. Initial hero totals are Eggs available, Birds remaining and Buyers still owe me. Detailed production, sales and costs are one step lower. Proposed navigation: Poultry, Buyers, Costs, Summary, More. Flock management is reached from the poultry home rather than adding a sixth tab.

| Record | Practical information | Effect |
|---|---|---|
| Flock or batch | Name, layers/broilers/mixed, start date, opening count | Establishes a separate stock record |
| Egg production | Date, flock, eggs collected, broken, home use | Changes egg stock; does not create a sale |
| Egg sale | Buyer, date, pieces or trays, agreed price, payment received | Reduces egg stock; posts sales and any buyer balance |
| Bird movement | Birds brought in, sold, lost, transferred or corrected; date and reason | Changes bird stock; only an actual sale posts sales |
| Collection | Buyer, amount, date and method | Reduces an existing balance; does not create another sale |
| Cost | Feed, chicks/birds purchased, veterinary, bedding, transport, labour or other | Follows the agreed recording policy; remains separate from production counts |

A tray size is explicit and configurable. The acceptance fixture below uses 30 eggs per tray. Home consumption, breakage and bird losses carry reasons and do not automatically invent a cash expense. Transfers between a farmer's poultry and shop records need a deliberate stock-transfer policy before they are introduced; they must not silently become external sales.

## Shared foundation

Reuse business-scoped IDs, party accounts, the common ledger, payment recording, expense records, period coverage, edit/void history, PIN, English/Kiswahili, whole-app encrypted backup and aggregate statement export. Introduce additive poultry production and flock-movement tables with an explicit migration; retain all existing R3 records.

Production records and inventory movements reference their source event. A sale posts once to the ledger. Correcting a sale or production record updates dependent stock atomically and preserves its original values, entry time and reason. Do not permit accidental negative egg or bird stock.

Poultry costs and biological stock need an agreed costing policy before any margin is offered. The first version reports sales, received payments, recorded costs, quantities and current buyer balances. A sales-less-recorded-expenses figure carries the same limitations as other goods businesses. Owner capital and loan proceeds remain separate from sales.

## SAT-P: layers acceptance fixture

All records below belong to a clearly labelled Demo Poultry business. Amounts are test values, not market-price recommendations.

| Step | Action | Expected result |
|---|---|---|
| P1 | Create a layer flock with 100 birds and zero opening eggs | 100 birds; 0 eggs available |
| P2 | Day 1: collect 90 eggs, record 3 broken and 2 used at home | 85 eggs available; no sales or receipts |
| P3 | Day 2: collect 88, record 2 broken and 1 used at home | 170 eggs available; no sales or receipts |
| P4 | Sell 4 trays of 30 eggs at KES 450 per tray; receive KES 1,000 | 50 eggs available; sales KES 1,800; received KES 1,000; owed KES 800 |
| P5 | Collect KES 300 later | Sales remain KES 1,800; received KES 1,300; owed KES 500 |
| P6 | Record feed cost KES 600 | Spent KES 600; sales less recorded expenses KES 1,200, with the costing limitation beside it |
| P7 | Record loss of 2 birds with a reason | 98 birds; no additional cash expense or sales |
| P8 | Correct Day 1 collected eggs from 90 to 92 with a reason | 52 eggs available; sales, receipts and owed unchanged; original 90 and reason visible |
| P9 | Switch to Dairy and Retail, then back to Poultry | Each business retains only its own quantities and money |
| P10 | Back up, restore in isolated storage on emulator-5554 and recover the test PIN | Flock, eggs, sales, collections, costs, corrections and balances match |
| P11 | Use Kiswahili, 130% font and chosen-period statement preview/export | Labels complete, controls reachable, quantities legible; default export excludes individuals |

Add a separate broiler acceptance fixture before calling broiler support complete: batch opening, bird loss, partial bird sale, part-payment and later collection, feed cost, remaining birds, correction and recovery. Never infer broiler completion from the layers scenario.

## Release conditions

Poultry is complete only after its native scripts, stock and ledger invariants, database migration, recovery and screenshots pass. Confirm Kiswahili poultry vocabulary with a native reviewer, then pilot with a real poultry keeper on the intended physical phone. The product records the owner's business; it provides no veterinary treatment advice, lending decision or promise of finance.
