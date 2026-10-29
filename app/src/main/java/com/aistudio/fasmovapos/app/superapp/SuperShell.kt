package com.aistudio.fasmovapos.app.superapp

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aistudio.fasmovapos.app.BackupTab
import com.aistudio.fasmovapos.app.R
import com.aistudio.fasmovapos.app.onboarding.TermsScreen
import com.aistudio.fasmovapos.app.ui.companion.LoanSupportScreen
import com.aistudio.fasmovapos.app.ui.components.DisplayControl
import com.aistudio.fasmovapos.app.ui.language.AppLanguage
import com.aistudio.fasmovapos.app.ui.screens.RecoveryDialog
import com.aistudio.fasmovapos.app.ui.screens.SetupScreen

/** Availability changes only after each actual workflow is implemented. */

fun tabsFor(type: String): List<String> =
  when (type) {
    "DAIRY" -> listOf("milk", "buyers", "costs", "summary", "more")
    "RENTAL" -> listOf("rent", "units", "costs", "summary", "more")
    "RESTAURANT" -> listOf("sell", "menu", "costs", "today", "more")
    else -> listOf("sell", "stock", "deni", "today", "more")
  }

@Composable
fun SuperChooser(
  onChoose: (Livelihood, Boolean) -> Unit,
  modifier: Modifier = Modifier,
  language: DisplayControl? = null,
) {
  var demo by rememberSaveable { mutableStateOf(false) }
  var recovery by remember { mutableStateOf(false) }
  Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      Text("Apen", fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
      language?.let {
        LanguageButtons(
          display = it,
        )
      }
    }
    Text(S("welcome"), style = MaterialTheme.typography.headlineMedium)
    Text(S("choose_work"), style = MaterialTheme.typography.titleLarge)
    Text(S("choose_help"))
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { demo = !demo }, verticalAlignment = Alignment.CenterVertically) {
      Column(Modifier.weight(1f)) {
        Text(S("demo_label"))
        Text(S("demo_help"))
      }
      Switch(demo, { demo = it })
    }
    Livelihood.entries.forEach { type ->
      val available = type in SuperAvailability.types
      LivelihoodTheme(type) {
        Card(onClick = {
          if (available)onChoose(type, demo)
        }, enabled = available, modifier = Modifier.fillMaxWidth().testTag("type-${type.name}")) {
          Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            LivelihoodArt(type, Modifier.size(100.dp, 84.dp))
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
              Text(S(type.name.lowercase()), style = MaterialTheme.typography.titleMedium)
              Text(
                S(
                  type.name.lowercase() + "_intro",
                ),
              )
              if (!available)Text(S("soon"))
            }
          }
        }
      }
    }
    TextButton(onClick = { recovery = true }, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.r2_recover)) }
  }
  if (recovery)RecoveryDialog { recovery = false }
}

@Composable
fun SuperFirstRun(
  isLoading: Boolean,
  display: DisplayControl,
  onComplete: (String, String, String, String, Boolean, String, String, Livelihood, Boolean) -> Unit,
) {
  var type by rememberSaveable { mutableStateOf<String?>(null) }
  var demo by rememberSaveable { mutableStateOf(false) }
  if (type == null) {
    SuperChooser(
      onChoose = { t, d ->
        type = t.name
        demo = d
      },
      language = display,
    )
  } else {
    val selected = Livelihood.valueOf(requireNotNull(type))
    LivelihoodTheme(selected) {
      BackHandler(enabled = !isLoading) { type = null }
      SetupScreen(
        isLoading,
        { business, owner, user, pin, _, phone, tax -> onComplete(business, owner, user, pin, false, phone, tax, selected, demo) },
        displayControl = display,
        scopeText =
          S(selected.name.lowercase()) + (if (demo)" · " + S("demo_banner") else ""),
      )
    }
  }
}

@Composable
fun LanguageButtons(
  display: DisplayControl,
  modifier: Modifier = Modifier,
) {
  Row(modifier) {
    TextButton(onClick = {
      display.onLanguageChange(AppLanguage.ENGLISH)
    }, modifier = Modifier.heightIn(min = 48.dp)) { Text("EN", fontSize = 16.sp) }
    TextButton(onClick = {
      display.onLanguageChange(AppLanguage.SWAHILI)
    }, modifier = Modifier.heightIn(min = 48.dp)) { Text("SW", fontSize = 16.sp) }
  }
}

@Composable
fun BusinessSwitch(
  profiles: UiList<BusinessProfile>,
  busy: Boolean,
  onDismiss: () -> Unit,
  onSelect: (String) -> Unit,
  onAdd: () -> Unit,
) {
  AlertDialog(onDismissRequest = onDismiss, title = { Text(S("switch_business")) }, text = {
    Column(Modifier.heightIn(max = 450.dp).verticalScroll(rememberScrollState())) {
      profiles.forEach { p ->
        TextButton(onClick = {
          onSelect(p.id)
        }, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("business-${p.id}")) {
          Column(Modifier.fillMaxWidth()) {
            Text(p.name)
            Text(S(p.type.lowercase()) + (if (p.demo)" · " + S("demo_banner") else ""))
          }
        }
      }
    }
  }, confirmButton = {
    Button(onClick = onAdd, enabled = !busy) {
      Text(S("add_business"))
    }
  }, dismissButton = { TextButton(onClick = onDismiss) { Text(S("close")) } })
}

@Composable
fun AddBusiness(
  actions: SuperActions,
  state: SuperUiState,
  onComplete: () -> Unit,
  display: DisplayControl,
) {
  var choice by remember { mutableStateOf<Livelihood?>(null) }
  var demo by remember { mutableStateOf(false) }
  if (choice == null) {
    BackHandler { onComplete() }
    SuperChooser(
      onChoose = { t, d ->
        choice = t
        demo = d
      },
      language = display,
    )
  } else {
    SuperForm(
      title = S("create"),
      fields = uiList(listOf(SuperField("name", S("business_name")))),
      busy = state.working,
      error = errorLabel(state.error),
      onDismiss = onComplete,
      onSave = { fields ->
        var created: String? = null
        actions.act(onSuccess = {
          created?.let(actions.choose)
          onComplete()
        }) { created = it.createBusiness(requireNotNull(choice), fields["name"].orEmpty(), demo).id }
      },
    )
  }
}

@Composable
fun errorLabel(key: String?): String? =
  key?.let {
    val known =
      setOf(
        "AMOUNT",
        "QUANTITY",
        "PARTY",
        "OVERPAYMENT",
        "STOCK",
        "REASON",
        "DATE",
        "HAS_ENTRIES",
        "IN_USE",
        "BUSINESS",
        "NAME",
        "DEDUCTION",
        "PHOTO",
      )
    S(if (it in known)"error_${it.lowercase()}" else "error")
  }

@Composable
fun SuperMainShell(
  displayControl: DisplayControl,
  onLogout: () -> Unit,
  user: com.aistudio.fasmovapos.app.data.db.entities.StaffUserEntity,
  modifier: Modifier = Modifier,
  vm: SuperViewModel = viewModel(),
) {
  val state by vm.state.collectAsStateWithLifecycle()
  val actions = vm.actions
  var switch by remember { mutableStateOf(false) }
  var add by remember { mutableStateOf(false) }
  LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refresh() }
  if (add || (!state.loading && state.businesses.isEmpty())) {
    AddBusiness(actions, state, { add = false }, displayControl)
    return
  }
  val data = state.data
  if (data == null) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    return
  }
  val type = Livelihood.valueOf(data.profile.type)
  LivelihoodTheme(type) {
    key(data.profile.id) {
      val tabs = tabsFor(data.profile.type)
      var tab by rememberSaveable { mutableStateOf(tabs.first()) }
      BackHandler(tab != tabs.first()) { tab = if (tab in tabs)tabs.first() else "more" }
      Scaffold(modifier = modifier, topBar = {
        BusinessHeader(data.profile, state.working, displayControl, onSwitch = { switch = true })
      }, bottomBar = {
        NavigationBar {
          tabs.forEach { t ->
            NavigationBarItem(selected = tab == t, onClick = {
              if (!state.working) {
                tab = t
                vm.clearError()
              }
            }, icon = {
              Icon(
                navigationIcon(t),
                contentDescription = null,
              )
            }, label = { Text(S(t), fontSize = 16.sp) }, modifier = Modifier.testTag("nav-$t"))
          }
        }
      }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
          if (tab !in tabs)TextButton(onClick = { tab = "more" }, modifier = Modifier.heightIn(min = 48.dp)) { Text(S("back")) }
          when (tab) {
            "more" ->
              SuperMore(
                type = data.profile.type,
                onNavigate = {
                  tab = it
                  vm.clearError()
                },
              )
            "backup" -> BackupTab(currentUser = user, includeLegacyCsv = false)
            "loans" -> LoanSupportScreen()
            "terms" -> TermsScreen(onAccept = { tab = "more" }, reviewOnly = true, onLanguageChange = displayControl.onLanguageChange)
            "language" ->
              Row(Modifier.padding(20.dp)) {
                LanguageButtons(
                  display = displayControl,
                )
              }
            "help" -> Text(S("help_body"), modifier = Modifier.padding(20.dp))
            "logout" -> {
              onLogout()
            }
            else ->
              SuperRoute(
                tab = tab,
                data = data,
                actions = actions,
                state = state,
                onNavigate = { tab = it },
              )
          }
        }
      }
    }
  }
  if (switch) {
    BusinessSwitch(
      uiList(state.businesses),
      state.working,
      { switch = false },
      {
        vm.choose(it)
        switch = false
      },
      {
        switch = false
        add = true
      },
    )
  }
}

@Composable
fun SuperMore(
  type: String,
  onNavigate: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Text(S("more"), style = MaterialTheme.typography.headlineMedium)
    (
      listOf("settings", "history", "backup", "language", "help", "terms", "loans") +
        (if (type == "RESTAURANT") listOf("deni") else emptyList()) +
        "logout"
    ).forEach { key ->
      OutlinedButton(onClick = {
        onNavigate(key)
      }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("more-$key")) { Text(S(key)) }
    }
  }
}

@Composable
fun SuperOwnerOnly(
  onLogout: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
    Text(S("owner_only"))
    Button(onClick = onLogout) { Text(S("logout")) }
  }
}

private fun navigationIcon(t: String): androidx.compose.ui.graphics.vector.ImageVector =
  when (t) {
    "milk" -> Icons.Default.WaterDrop
    "rent", "units" -> Icons.Default.HomeWork
    "buyers", "deni" -> Icons.Default.Groups
    "sell" -> Icons.Default.ShoppingCart
    "stock" -> Icons.Default.Inventory2
    "menu" -> Icons.Default.MenuBook
    "costs" -> Icons.Default.Payments
    "summary", "today" -> Icons.Default.Assessment
    else -> Icons.Default.MoreHoriz
  }

@Composable
private fun BusinessHeader(
  profile: BusinessProfile,
  busy: Boolean,
  displayControl: DisplayControl,
  onSwitch: () -> Unit,
) {
  Column {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
      TextButton(onClick = {
        onSwitch()
      }, enabled = !busy, modifier = Modifier.weight(1f).heightIn(min = 56.dp).testTag("business-switch")) {
        Text(
          profile.name + " ▾",
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          fontSize = 18.sp,
        )
      }
      LanguageButtons(
        display = displayControl,
      )
    }
    if (profile.demo)Text(S("demo_banner"), modifier = Modifier.padding(horizontal = 20.dp), fontWeight = FontWeight.Bold)
    if (busy)Text(S("working"), modifier = Modifier.padding(horizontal = 20.dp))
  }
}
