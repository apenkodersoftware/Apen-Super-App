package com.aistudio.fasmovapos.app.superapp

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun RetailCheckout(
  data: BusinessData,
  cart: UiMap<String, Long>,
  state: SuperUiState,
  actions: SuperActions,
  dismiss: () -> Unit,
  saved: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val items = data.items.associateBy { it.id }
  val gross = cart.entries.sumOf { (id, q) -> SuperPricing.lineTotal(items.getValue(id), q) }
  val parties = data.parties.filter { !it.voided && it.kind == "CUSTOMER" }
  val fields =
    listOf(
      SuperField("discount", S("discount"), "0", FieldKind.AMOUNT),
      SuperField("received", S("received_now"), "", FieldKind.AMOUNT),
      SuperField("party", S("customer"), "NONE", FieldKind.CHOICE, listOf("NONE" to S("no_customer")) + parties.map { it.id to it.name }),
      SuperField("date", S("date"), SuperDates.today(), FieldKind.DATE),
      SuperField(
        "method",
        S("method"),
        "CASH",
        FieldKind.CHOICE,
        optionLabels(
          uiList(listOf("CASH", "MPESA", "BANK")),
        ),
      ),
      SuperField("reference", S("reference")),
    )
  SuperForm(
    title = S("checkout"),
    fields = uiList(fields),
    busy = state.working,
    error = errorLabel(state.error),
    onDismiss = dismiss,
    onSave = { f ->
      val submitted = cart.toMap()
      actions.act(onSuccess = saved) {
        val discount = SuperMoney.cents(f.getValue("discount"))
        require(discount < gross) { "AMOUNT" }
        val received = if (f.getValue("received").isBlank()) gross - discount else SuperMoney.cents(f.getValue("received"))
        it.commerce.sale(
          data.profile.id,
          submitted,
          received,
          f.getValue("date"),
          if (received == 0L) "CREDIT" else f.getValue("method"),
          f.getValue("party").takeUnless { value -> value == "NONE" },
          f.getValue("reference"),
          discount,
        )
      }
    },
    extra = {
      Text(S("full_if_blank"))
      Text(S("customer_from_deni"))
    },
    preview = { f ->
      val discount = runCatching { SuperMoney.cents(f["discount"].orEmpty()) }.getOrNull()
      if (discount != null && discount <= gross) Text(S("net_total") + ": " + SuperMoney.label(gross - discount))
    },
    modifier = modifier,
  )
}
