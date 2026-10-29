package com.aistudio.fasmovapos.app.superapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/** A bounded local copy is part of the database backup; no network image or durable URI permission. */
private fun menuPhoto(
  context: Context,
  uri: Uri,
): ByteArray {
  val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
  context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, options) }
  require(options.outWidth > 0 && options.outHeight > 0) { "PHOTO" }
  var sample = 1
  while (maxOf(options.outWidth, options.outHeight) / sample > 512) sample *= 2
  options.inJustDecodeBounds = false
  options.inSampleSize = sample
  val bitmap =
    context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, options) }
      ?: throw IllegalArgumentException("PHOTO")
  return try {
    var quality = 80
    var bytes: ByteArray
    do {
      bytes =
        ByteArrayOutputStream().use { out ->
          bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
          out.toByteArray()
        }
      quality -= 15
    } while (bytes.size > 64 * 1024 && quality >= 20)
    require(bytes.size <= 64 * 1024) { "PHOTO" }
    bytes
  } finally {
    bitmap.recycle()
  }
}

@Composable
fun MenuEditor(
  data: BusinessData,
  item: CommerceItem?,
  actions: SuperActions,
  state: SuperUiState,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var photo by remember(item?.id) { mutableStateOf(item?.photo) }
  var reading by remember { mutableStateOf(false) }
  var photoError by remember { mutableStateOf(false) }
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val picker =
    rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
      if (uri != null) {
        scope.launch {
          reading = true
          photoError = false
          try {
            photo = withContext(Dispatchers.IO) { menuPhoto(context, uri) }
          } catch (_: java.io.IOException) {
            photoError = true
          } catch (_: IllegalArgumentException) {
            photoError = true
          } catch (_: SecurityException) {
            photoError = true
          } finally {
            reading = false
          }
        }
      }
    }
  val fields =
    mutableListOf(
      SuperField("name", S("name"), item?.name.orEmpty()),
      SuperField("price", S("price"), item?.let { inputMoney(it.priceCents) }.orEmpty(), FieldKind.AMOUNT),
      SuperField(
        "category",
        S("category"),
        item?.category ?: "FOOD",
        FieldKind.CHOICE,
        optionLabels(
          uiList(listOf("FOOD", "DRINKS", "SNACKS")),
        ),
      ),
    )
  if (item != null) {
    fields += SuperField("archived", S("archive_menu"), item.archived.toString(), FieldKind.FLAG)
    fields += SuperField("reason", S("reason"))
  }
  SuperForm(
    title = S(if (item == null) "add_menu" else "edit"),
    fields = uiList(fields),
    busy = state.working || reading,
    error = errorLabel(state.error),
    onDismiss = onDismiss,
    onSave = { values ->
      actions.act(onSuccess = onDismiss) { repo ->
        repo.store.transaction {
          val id =
            item?.id
              ?: repo.item(
                data.profile.id,
                values.getValue("name"),
                SuperMoney.cents(values.getValue("price")),
                values.getValue("category"),
              )
          repo.commerce.menuDetails(
            id,
            values.getValue("name"),
            SuperMoney.cents(values.getValue("price")),
            values.getValue("category"),
            photo,
            values["archived"] == "true",
            if (item == null) "Initial menu details" else values["reason"].orEmpty(),
          )
        }
      }
    },
    extra = {
      Column {
        OutlinedButton(
          onClick = { picker.launch("image/*") },
          enabled = !reading && !state.working,
          modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) { Text(S(if (reading) "working" else "choose_photo")) }
        if (photo != null) {
          OutlinedButton(
            onClick = { photo = null },
            enabled = !reading && !state.working,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
          ) { Text(S("remove_photo")) }
        }
        if (photoError) Text(S("photo_error"))
      }
    },
    modifier = modifier,
  )
}
