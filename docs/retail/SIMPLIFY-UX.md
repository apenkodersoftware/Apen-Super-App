# MaliMali: simple interface review

Phase 1 changes the interface while retaining the existing database, transaction methods, owner/PIN authentication, signing, app ID and cloud configuration.

| Before | Simple mode |
| --- | --- |
| Hidden navigation and separate cart tab | Sell / Stock / Today / More, with a sticky Charge bar |
| Header with owner, role, branch and several icons | Shop name and EN / SW |
| Product cards with SKU and category metadata | Name, price and stock left; tap the card to add |
| Customer, split methods and discount controls always visible | Cash, M-Pesa or Deni; customer only for Deni, discount collapsed |
| Large inventory form | Name, Price, Quantity first; details expandable |
| Broad reports dashboard | Today's total, count, payment breakdown and receipts |
| Staff and settings mixed into the main flow | Business utilities and settings under More |

The default-on `SimpleMode.ENABLED` flag is in `app/src/main/java/com/aistudio/fasmovapos/app/ui/simple/SimpleMode.kt`. Set it to `false`, rebuild and update-install to return to the complete interface. Existing screens and business logic are retained.

## Scope decisions

- M-Pesa phone is optional. Its code remains required because PaymentState, the existing confirmer and repository reject missing codes. Relaxing that rule would require a separate payment policy change.
- A product has no supplier relationship in the current schema. Supplier management remains under More; no association or schema was invented.
- Existing-item editing uses the existing stock adjustment API for quantity, price and cost. Product renaming is unavailable through that API.
- First-run owner/PIN setup is retained. Replacing it with shop-name-only setup would change authentication and falls outside this UI phase.
- Earlier sales using Airtel, Card or split payments remain readable. Those new-sale choices, staff controls, branch/user switching and advanced dashboards are accessible through the complete interface with SimpleMode off.
- Saved payment records remain records; M-Pesa confirmation is manual. This build does not establish payment-provider integration or production readiness.

## Before Monday's demonstration

1. Confirm Sell, Stock, Today and More in English and Kiswahili.
2. Use your own sample items to check name, price, stock warning and low-stock ordering.
3. Complete an intentional test Cash sale, inspect the receipt and restart to confirm the saved sale and stock count.
4. Check M-Pesa with a valid unique reference and Deni with a selected customer, including repayment under More.
5. Check discount totals against your expected amounts; the original calculation code is unchanged.
6. Review at 360dp and a larger system font, and check the keyboard does not hide the primary action.
7. Verify Backup with a safe copy and a password you know; do not restore over the demonstration data without a separate plan.

Automated gates and the emulator inspection described in the final verification report cover only their stated checks. The business acceptance actions above remain the owner's review.