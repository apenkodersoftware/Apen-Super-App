package com.aistudio.fasmovapos.app.superapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import java.math.BigDecimal

fun inputMoney(cents: Long): String = BigDecimal(cents).movePointLeft(2).stripTrailingZeros().toPlainString()

@Composable
fun EmptyAction(
  type: Livelihood,
  text: String,
  action: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    LivelihoodArt(type, Modifier.size(150.dp, 110.dp))
    Text(text)
    Button(onClick = onClick, modifier = Modifier.heightIn(min = 48.dp)) { Text(action) }
  }
}

@Composable
fun PartyScreen(
  data: BusinessData,
  actions: SuperActions,
  state: SuperUiState,
  modifier: Modifier = Modifier,
) {
  var adding by remember { mutableStateOf(false) }
  var selected by remember { mutableStateOf<String?>(null) }
  var paying by remember { mutableStateOf(false) }
  val dairy = data.profile.type == "DAIRY"
  val type = Livelihood.valueOf(data.profile.type)
  val parties = data.parties.filter { !it.voided && it.kind != "TENANT" }
  val party = parties.firstOrNull { it.id == selected }
  if (party == null) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Text(S(if (dairy)"buyers" else "customers"), style = MaterialTheme.typography.headlineMedium)
      Button(
        onClick = {
          adding = true
          actions.clearError()
        },
        modifier =
          Modifier
            .fillMaxWidth()
            .heightIn(
              min = 48.dp,
            ).testTag("add-party"),
      ) { Text(S(if (dairy)"add_buyer" else "add_customer")) }
      if (parties.isEmpty()) {
        EmptyAction(
          type = type,
          text = S(if (dairy)"empty_buyers" else "empty_customers"),
          action = S(if (dairy)"add_buyer" else "add_customer"),
          onClick = {
            adding =
              true
          },
        )
      }
      parties.forEach { p ->
        Card(onClick = { selected = p.id }, modifier = Modifier.fillMaxWidth().testTag("party-${p.name}")) {
          Column(Modifier.padding(16.dp)) {
            Text(p.name, style = MaterialTheme.typography.titleMedium)
            if (p.phone.isNotBlank())Text(p.phone)
            Text(
              S("owed") + ": " + SuperMoney.label(data.partyBalance(p.id)),
            )
          }
        }
      }
    }
  } else {
    PartyAccount(data, party, actions, state, onBack = { selected = null }, onPay = { paying = true })
  }
  if (adding) {
    val fields = mutableListOf(SuperField("name", S("name")), SuperField("phone", S("phone")))
    if (dairy) {
      fields +=
        listOf(
          SuperField(
            "kind",
            S("buyer_type"),
            "COOPERATIVE",
            FieldKind.CHOICE,
            optionLabels(
              uiList(listOf("COOPERATIVE", "PROCESSOR", "BROKER", "HOTEL", "NEIGHBOUR")),
            ),
          ),
          SuperField("price", S("price_litre"), "50", FieldKind.AMOUNT),
          SuperField(
            "cycle",
            S("payment_cycle"),
            "MONTHLY",
            FieldKind.CHOICE,
            optionLabels(
              uiList(listOf("DAILY", "WEEKLY", "MONTHLY")),
            ),
          ),
        )
    }
    SuperForm(
      title = S(if (dairy)"add_buyer" else "add_customer"),
      fields = uiList(fields),
      busy = state.working,
      error = errorLabel(state.error),
      onDismiss = { adding = false },
      onSave = { f ->
        actions.act(onSuccess = { adding = false }) {
          it.addParty(
            data.profile.id,
            f["name"].orEmpty(),
            f["kind"] ?: "CUSTOMER",
            if (dairy)SuperMoney.cents(f["price"].orEmpty())else 0,
            f["phone"].orEmpty(),
            f["cycle"] ?: "MONTHLY",
          )
        }
      },
    )
  }
  if (paying && party != null) {
    ReceiptDialog(
      data = data,
      party = party,
      state = state,
      actions = actions,
      onDismiss = { paying = false },
    )
  }
}

@Composable
private fun PartyAccount(
  data: BusinessData,
  party: Party,
  actions: SuperActions,
  state: SuperUiState,
  onBack: () -> Unit,
  onPay: () -> Unit,
) {
  val dairy = data.profile.type == "DAIRY"
  val ledger = data.ledger.filter { !it.voided && it.partyId == party.id }
  Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    TextButton(onClick = { onBack() }) { Text(S("back")) }
    Text(party.name, style = MaterialTheme.typography.headlineMedium)
    if (party.phone.isNotBlank())Text(party.phone)
    if (dairy)Text(S("price_litre") + ": " + SuperMoney.label(party.priceCents))
    HeroNumber(S(if (dairy)"earned" else "sales"), SuperMoney.label(ledger.sumOf { it.earnedCents }))
    HeroNumber(S("received"), SuperMoney.label(ledger.sumOf { it.receivedCents }))
    if (dairy)HeroNumber(S("deductions"), SuperMoney.label(ledger.sumOf { it.settledCents }))
    HeroNumber(S("owed"), SuperMoney.label(data.partyBalance(party.id)))
    Button(onClick = {
      onPay()
      actions.clearError()
    }, enabled = data.partyBalance(party.id) > 0, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("record-payment")) {
      Text(S("payment"))
    }
    data.receipts.filter { !it.voided && it.partyId == party.id }.sortedByDescending { it.occurredOn }.forEach { r ->
      Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
          Text(dateLabel(r.occurredOn))
          Text(SuperMoney.label(r.amountCents))
          Text(
            S(
              "option_" + r.method.lowercase(),
            ),
          )
          if (r.reference.isNotBlank())Text(r.reference)
        }
      }
    }
    if (dairy) {
      MilkHistoryList(
        rows = uiList(data.milk.filter { !it.voided && it.partyId == party.id }),
        data = data,
        actions = actions,
        state = state,
      )
    }
  }
}

data class DraftDeduction(
  val category: String = "FEED",
  val amount: String = "",
  val cost: Boolean = true,
)

@Composable
fun ReceiptDialog(
  data: BusinessData,
  party: Party,
  state: SuperUiState,
  actions: SuperActions,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var deductions by remember { mutableStateOf<List<DraftDeduction>>(emptyList()) }
  val dairy = data.profile.type == "DAIRY"
  SuperForm(
    title = S("payment") + ": " + party.name,
    fields =
      uiList(
        listOf(
          SuperField("amount", S("received_now"), inputMoney(data.partyBalance(party.id)), FieldKind.AMOUNT),
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
        ),
      ),
    busy = state.working,
    error = errorLabel(state.error),
    onDismiss = onDismiss,
    onSave = { f ->
      val submittedDeductions = deductions.toList()
      actions.act(
        onSuccess = onDismiss,
      ) { repo ->
        repo.receipt(
          data.profile.id,
          party.id,
          SuperMoney.cents(
            f["amount"].orEmpty(),
          ),
          f["date"].orEmpty(),
          f["method"].orEmpty(),
          f["reference"].orEmpty(),
          submittedDeductions.map {
            Deduction(it.category, SuperMoney.cents(it.amount), it.cost)
          },
        )
      }
    },
    extra = {
      if (dairy) {
        Text(S("deduction_help"))
        deductions.forEachIndexed { index, d ->
          Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              ChoiceField(
                S(
                  "category",
                ),
                d.category,
                uiList(
                  optionLabels(
                    uiList(listOf("FEED", "VET_AI", "SHARES", "ADVANCE", "LOAN_RECOVERY", "OTHER")),
                  ),
                ),
                { category ->
                  deductions =
                    deductions.toMutableList().also {
                      it[index] =
                        d.copy(
                          category = category,
                          cost = category !in setOf("SHARES", "ADVANCE", "LOAN_RECOVERY"),
                        )
                    }
                },
                enabled = !state.working,
              )
              OutlinedTextField(
                d.amount,
                { amount ->
                  deductions =
                    deductions.toMutableList().also {
                      it[index] = d.copy(amount = amount)
                    }
                },
                enabled = !state.working,
                label = {
                  Text(
                    S("amount"),
                  )
                },
                textStyle = MaterialTheme.typography.bodyLarge,
                keyboardOptions =
                  androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal,
                  ),
                modifier = Modifier.testTag("deduction-amount-$index"),
              )
              Row(Modifier.heightIn(min = 48.dp)) {
                Checkbox(
                  d.cost,
                  { cost -> deductions = deductions.toMutableList().also { it[index] = d.copy(cost = cost) } },
                  enabled =
                    !state.working && d.category !in setOf("SHARES", "ADVANCE", "LOAN_RECOVERY"),
                )
                ;Text(S("counts_cost"))
              }
              TextButton(onClick = {
                deductions =
                  deductions.filterIndexed {
                    i,
                    _,
                    ->
                    i != index
                  }
              }, enabled = !state.working) { Text(S("cancel")) }
            }
          }
        }
        OutlinedButton(onClick = {
          deductions = deductions + DraftDeduction()
        }, enabled = !state.working, modifier = Modifier.heightIn(min = 48.dp)) { Text(S("add_deduction")) }
        Text(S("noncost_deduction"))
      }
      Text(S("oldest_first"))
    },
    modifier = modifier,
  )
}

@Composable
fun CostsScreen(
  data: BusinessData,
  actions: SuperActions,
  state: SuperUiState,
  modifier: Modifier = Modifier,
) {
  var draft by remember { mutableStateOf<ExpenseRow?>(null) }
  var quickCategory by remember { mutableStateOf<String?>(null) }
  var adding by remember { mutableStateOf(false) }
  val categories = costCategories(data.profile.type)
  Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text(S(if (data.profile.type == "RESTAURANT")"shopping" else "costs"), style = MaterialTheme.typography.headlineMedium)
    Button(onClick = {
      draft = null
      quickCategory = null
      adding = true
      actions.clearError()
    }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("add-cost")) { Text(S("add_cost")) }
    if (data.profile.type == "RESTAURANT") {
      categories.chunked(2).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          row.forEach { category ->
            OutlinedButton(
              onClick = {
                draft = null
                quickCategory = category
                adding = true
                actions.clearError()
              },
              enabled = !state.working,
              modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            ) { Text(S("option_" + category.lowercase())) }
          }
        }
      }
    }
    if (data.expenses.none { !it.voided }) {
      EmptyAction(
        type = Livelihood.valueOf(data.profile.type),
        text = S("empty_costs"),
        action = S("add_cost"),
        onClick = { adding = true },
      )
    }
    data.expenses.filter { !it.voided }.sortedByDescending { it.occurredOn }.forEach { e ->
      Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            if (e.category in
              categories
            ) {
              S("option_" + e.category.lowercase())
            } else {
              e.category
            },
            style = MaterialTheme.typography.titleMedium,
          )
          Text(SuperMoney.label(e.amountCents))
          Text(dateLabel(e.occurredOn))
          if (e.note.isNotBlank())Text(e.note)
          TextButton(onClick = {
            draft = e
            adding = true
            actions.clearError()
          }, modifier = Modifier.heightIn(min = 48.dp)) { Text(S("buy_again")) }
        }
      }
    }
  }
  if (adding) {
    val fields =
      mutableListOf(
        SuperField(
          "category",
          S("category"),
          draft?.category?.takeIf {
            it in categories
          } ?: quickCategory ?: categories.first(),
          FieldKind.CHOICE,
          optionLabels(
            uiList(categories),
          ),
        ),
        SuperField(
          "amount",
          S("amount"),
          draft?.let { inputMoney(it.amountCents) } ?: "",
          FieldKind.AMOUNT,
        ),
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
        SuperField("note", S("note")),
      )
    if (data.profile.type == "RENTAL" &&
      data.properties.isNotEmpty()
    ) {
      fields +=
        SuperField(
          "property",
          S("property"),
          draft?.propertyId ?: data.properties.first().id,
          FieldKind.CHOICE,
          data.properties.map {
            it.id to
              it.name
          },
        )
    }
    SuperForm(
      title = S("add_cost"),
      fields = uiList(fields),
      busy = state.working,
      error = errorLabel(state.error),
      onDismiss = {
        adding = false
      },
      onSave = { f ->
        actions.act(onSuccess = {
          adding = false
        }) {
          it.expense(
            data.profile.id,
            SuperMoney.cents(f["amount"].orEmpty()),
            f["date"].orEmpty(),
            f["category"].orEmpty(),
            f["method"].orEmpty(),
            f["reference"].orEmpty(),
            f["note"].orEmpty(),
            f["property"],
          )
        }
      },
    )
  }
}
