package com.aistudio.fasmovapos.app.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.fasmovapos.app.R
import com.aistudio.fasmovapos.app.ui.language.AppLanguage

/** Revision-gated acceptance; the same text can be reviewed later without changing consent. */
@Composable
fun TermsScreen(
  onAccept: () -> Unit,
  modifier: Modifier = Modifier,
  reviewOnly: Boolean = false,
  onLanguageChange: (AppLanguage) -> Unit = {},
) {
  var checked by rememberSaveable(TermsPreferences.CURRENT_REVISION) { mutableStateOf(false) }
  Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
    Column(
      modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
        TextButton(onClick = {
          onLanguageChange(AppLanguage.ENGLISH)
        }, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.simple_en), fontSize = 16.sp) }
        Text(stringResource(R.string.simple_language_separator), fontSize = 16.sp)
        TextButton(onClick = {
          onLanguageChange(AppLanguage.SWAHILI)
        }, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.simple_sw), fontSize = 16.sp) }
      }
      Text(stringResource(R.string.terms_title), fontSize = 22.sp, lineHeight = 30.sp)
      if (!reviewOnly) Text(stringResource(R.string.terms_updated_notice), fontSize = 16.sp, lineHeight = 24.sp)
      Text(
        text = stringResource(R.string.terms_body),
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        modifier =
          Modifier
            .weight(1f)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .testTag("terms_body"),
      )
      if (!reviewOnly) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
          Checkbox(checked = checked, onCheckedChange = { checked = it }, modifier = Modifier.testTag("terms_checkbox"))
          Text(stringResource(R.string.terms_accept_checkbox), fontSize = 16.sp, lineHeight = 24.sp, modifier = Modifier.weight(1f))
        }
      }
      Button(
        onClick = onAccept,
        enabled = reviewOnly || checked,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("terms_continue"),
      ) {
        Text(stringResource(if (reviewOnly) R.string.simple_close else R.string.terms_continue), fontSize = 16.sp)
      }
    }
  }
}
