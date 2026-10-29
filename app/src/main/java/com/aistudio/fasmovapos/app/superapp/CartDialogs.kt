package com.aistudio.fasmovapos.app.superapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

data class CartDialogs(
  val quantityItem: CommerceItem?,
  val payment: Boolean,
  val custom: Boolean,
  val checkout: Boolean,
  val cart: UiMap<String, Long>,
)

@Composable
internal fun CommerceDialogs(
  data: BusinessData,
  actions: SuperActions,
  state: SuperUiState,
  dialogs: CartDialogs,
  onDismiss: () -> Unit,
  onFinish: () -> Unit,
  onQuantity: (String, Long) -> Unit,
) {
  dialogs.quantityItem?.let { item ->
    QuantityDialog(item, dialogs.cart[item.id] ?: 1000L, state, actions, { onDismiss() }) { q ->
      onQuantity(item.id, q)
    }
  }
  if (dialogs.payment) {
    CommercePayment(
      data,
      dialogs.cart,
      state,
      actions,
      { onDismiss() },
      { onFinish() },
    )
  }
  if (dialogs.custom) CustomAmountDialog(data, state, actions, { onDismiss() }, { onFinish() })
  if (dialogs.checkout) {
    RetailCheckout(
      data = data,
      cart = dialogs.cart,
      state = state,
      actions = actions,
      dismiss = { onDismiss() },
      saved = { onFinish() },
    )
  }
}

@Composable
private fun QuantityDialog(
  item: CommerceItem,
  current: Long,
  state: SuperUiState,
  actions: SuperActions,
  dismiss: () -> Unit,
  save: (Long) -> Unit,
) {
  SuperForm(
    title = S("quantity") + ": " + item.name,
    fields = uiList(listOf(SuperField("quantity", S("quantity"), SuperMoney.quantity(current), FieldKind.AMOUNT))),
    busy = state.working,
    error = errorLabel(state.error),
    onDismiss = dismiss,
    onSave = { values ->
      try {
        val quantity = SuperMoney.milli(values.getValue("quantity"))
        require(item.unit !in setOf("Pcs", "Box", "Bag") || quantity % 1000L == 0L)
        save(quantity)
      } catch (_: IllegalArgumentException) {
        actions.act { throw IllegalArgumentException("QUANTITY") }
      }
    },
  )
}

@Composable
private fun CommercePayment(
  data: BusinessData,
  cart: UiMap<String, Long>,
  state: SuperUiState,
  actions: SuperActions,
  dismiss: () -> Unit,
  saved: () -> Unit,
) {
  val parties = data.parties.filter { !it.voided && it.kind == "CUSTOMER" }
  if (parties.isEmpty()) {
    SuperForm(
      title = S("add_customer"),
      fields = uiList(listOf(SuperField("name", S("name")))),
      busy = state.working,
      error = errorLabel(state.error),
      onDismiss = dismiss,
      onSave = { f ->
        actions.act { it.addParty(data.profile.id, f.getValue("name"), "CUSTOMER") }
      },
    )
  } else {
    val fields =
      listOf(
        SuperField("party", S("customer"), parties.first().id, FieldKind.CHOICE, parties.map { it.id to it.name }),
        SuperField("received", S("received_now"), "0", FieldKind.AMOUNT),
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
      title = S("part_payment"),
      fields = uiList(fields),
      busy = state.working,
      error = errorLabel(state.error),
      onDismiss = dismiss,
      onSave = { f ->
        val submitted = cart.toMap()
        actions.act(onSuccess = saved) {
          val amount = SuperMoney.cents(f.getValue("received"))
          it.commerce.sale(
            data.profile.id,
            submitted,
            amount,
            f.getValue("date"),
            if (amount == 0L) "CREDIT" else f.getValue("method"),
            f.getValue("party"),
            f.getValue("reference"),
          )
        }
      },
    )
  }
}

@Composable
private fun CustomAmountDialog(
  data: BusinessData,
  state: SuperUiState,
  actions: SuperActions,
  dismiss: () -> Unit,
  saved: () -> Unit,
) {
  SuperForm(
    title = S("custom_amount"),
    fields =
      uiList(
        listOf(
          SuperField("amount", S("amount"), kind = FieldKind.AMOUNT),
          SuperField(
            "method",
            S("method"),
            "CASH",
            FieldKind.CHOICE,
            optionLabels(
              uiList(listOf("CASH", "MPESA", "BANK")),
            ),
          ),
        ),
      ),
    busy = state.working,
    error = errorLabel(state.error),
    onDismiss = dismiss,
    onSave = { f ->
      actions.act(onSuccess = saved) {
        it.commerce.customSale(data.profile.id, SuperMoney.cents(f.getValue("amount")), SuperDates.today(), f.getValue("method"))
      }
    },
  )
}
