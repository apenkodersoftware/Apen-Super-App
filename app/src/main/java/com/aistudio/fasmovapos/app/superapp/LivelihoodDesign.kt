package com.aistudio.fasmovapos.app.superapp

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Original, bundled vector drawing. No borrowed artwork and no network image requests. */

fun palette(type: Livelihood) =
  when (type) {
    Livelihood.DAIRY -> LivelihoodPalette(Color(0xFF24553A), Color(0xFFFAFBF2), Color(0xFFE4EED9), Color(0xFF20392B))
    Livelihood.RENTAL -> LivelihoodPalette(Color(0xFF314B70), Color(0xFFFAF7F0), Color(0xFFE4EAF4), Color(0xFF23344C))
    Livelihood.RESTAURANT -> LivelihoodPalette(Color(0xFF873E22), Color(0xFFFFF8EE), Color(0xFFF8E1C8), Color(0xFF472D21))
    Livelihood.RETAIL -> LivelihoodPalette(Color(0xFF125B72), Color(0xFFF3FAFC), Color(0xFFDCEEF3), Color(0xFF203D48))
  }

@Composable
fun LivelihoodTheme(
  type: Livelihood,
  content: @Composable () -> Unit,
) {
  val p = palette(type)
  MaterialTheme(
    colorScheme =
      lightColorScheme(
        primary = p.accent,
        onPrimary = Color.White,
        primaryContainer = p.tint,
        onPrimaryContainer = p.ink,
        background = p.background,
        onBackground = p.ink,
        surface = Color.White,
        onSurface = p.ink,
        surfaceVariant = p.tint,
        surfaceContainerHighest = p.tint,
        surfaceContainer = p.background,
        surfaceContainerLow = p.background,
        surfaceContainerHigh = p.tint,
        secondaryContainer = p.tint,
        onSecondaryContainer = p.ink,
        surfaceTint = p.accent,
        onSurfaceVariant = p.ink,
        outline = p.accent,
      ),
    typography =
      Typography(
        bodyLarge = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 23.sp),
        bodyMedium = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp, lineHeight = 23.sp),
        bodySmall = MaterialTheme.typography.bodySmall.copy(fontSize = 16.sp, lineHeight = 22.sp),
        labelLarge = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp, lineHeight = 22.sp),
        labelMedium = MaterialTheme.typography.labelMedium.copy(fontSize = 16.sp, lineHeight = 22.sp),
        titleMedium = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold),
      ),
    shapes = Shapes(medium = RoundedCornerShape(18.dp), large = RoundedCornerShape(24.dp)),
    content = content,
  )
}

@Composable
fun LivelihoodArt(
  type: Livelihood,
  modifier: Modifier = Modifier,
) {
  Canvas(modifier = modifier) {
    val sx = size.width / 200f
    val sy = size.height / 150f
    with(drawContext.canvas) {
      save()
      scale(sx, sy)
      when (type) {
        Livelihood.DAIRY -> dairyArt()
        Livelihood.RENTAL -> rentalArt()
        Livelihood.RESTAURANT -> foodArt()
        Livelihood.RETAIL -> retailArt()
      }
      restore()
    }
  }
}

private fun DrawScope.dairyArt() {
  drawOval(Color(0xFFC9DCA8), Offset(5f, 120f), Size(190f, 25f))
  drawRoundRect(Color(0xFFF9F6E9), Offset(18f, 46f), Size(107f, 67f), CornerRadius(25f, 25f))
  drawOval(Color(0xFF44513D), Offset(45f, 50f), Size(32f, 34f))
  drawOval(Color(0xFF44513D), Offset(82f, 74f), Size(30f, 24f))
  drawLine(Color(0xFF46523C), Offset(38f, 100f), Offset(35f, 129f), 10f)
  drawLine(Color(0xFF46523C), Offset(104f, 100f), Offset(107f, 129f), 10f)
  drawOval(Color(0xFFF5F1E2), Offset(5f, 34f), Size(48f, 58f))
  drawOval(Color(0xFFB97563), Offset(3f, 67f), Size(49f, 24f))
  drawCircle(Color(0xFF29392C), 3f, Offset(17f, 55f))
  drawCircle(Color(0xFF29392C), 3f, Offset(40f, 55f))
  drawLine(Color(0xFF9B7954), Offset(15f, 40f), Offset(10f, 24f), 5f)
  drawLine(Color(0xFF9B7954), Offset(44f, 40f), Offset(49f, 25f), 5f)
  drawRoundRect(Color(0xFFADC7C4), Offset(130f, 74f), Size(54f, 62f), CornerRadius(9f, 9f))
  drawRect(Color(0xFFDCE9DF), Offset(142f, 54f), Size(30f, 22f))
  drawRoundRect(Color(0xFF436658), Offset(139f, 50f), Size(36f, 8f), CornerRadius(3f, 3f))
  drawLine(Color.White, Offset(141f, 85f), Offset(141f, 122f), 4f)
}

private fun DrawScope.rentalArt() {
  drawCircle(Color(0xFFF0D8A9), 25f, Offset(164f, 28f))
  drawRoundRect(Color(0xFFC2CFE2), Offset(16f, 30f), Size(79f, 109f), CornerRadius(5f, 5f))
  drawRect(Color(0xFF354E70), Offset(13f, 24f), Size(85f, 10f))
  drawRoundRect(Color(0xFFDDD1B8), Offset(102f, 59f), Size(78f, 80f), CornerRadius(5f, 5f))
  repeat(3) { row -> repeat(2) { col -> drawRect(Color(0xFFFDF7DC), Offset(29f + col * 34f, 43f + row * 27f), Size(18f, 16f)) } }
  repeat(2) { row -> repeat(2) { col -> drawRect(Color(0xFF698398), Offset(115f + col * 34f, 72f + row * 27f), Size(18f, 16f)) } }
  drawRect(Color(0xFF354E70), Offset(47f, 113f), Size(22f, 26f))
  drawLine(Color(0xFF7D8254), Offset(3f, 140f), Offset(194f, 140f), 7f)
}

private fun DrawScope.foodArt() {
  drawOval(Color(0xFFE6CAA4), Offset(9f, 124f), Size(182f, 19f))
  val bowl =
    Path().apply {
      moveTo(23f, 78f)
      cubicTo(25f, 145f, 160f, 145f, 170f, 78f)
      close()
    }
  drawPath(bowl, Color(0xFFB45B35))
  drawOval(Color(0xFFF5E4C6), Offset(21f, 62f), Size(151f, 40f))
  drawOval(Color(0xFFDFB64F), Offset(34f, 68f), Size(123f, 26f))
  repeat(8) { i ->
    drawCircle(
      if (i % 2 ==
        0
      ) {
        Color(0xFF49633E)
      } else {
        Color(0xFF86432E)
      },
      6f,
      Offset(48f + (i % 4) * 29f, 76f + (i / 4) * 10f),
    )
  }
  repeat(3) { i ->
    val p =
      Path().apply {
        moveTo(63f + i * 28f, 57f)
        cubicTo(46f + i * 28f, 37f, 80f + i * 28f, 29f, 64f + i * 28f, 10f)
      }
    drawPath(p, Color(0xFF9B7E67), style = Stroke(4f))
  }
  drawLine(Color(0xFF6B4C39), Offset(163f, 93f), Offset(190f, 30f), 7f)
  drawOval(Color(0xFFAA8862), Offset(176f, 12f), Size(21f, 34f))
}

private fun DrawScope.retailArt() {
  drawRoundRect(Color(0xFFD9E8E6), Offset(22f, 49f), Size(155f, 91f), CornerRadius(5f, 5f))
  drawRect(Color(0xFF2D6474), Offset(29f, 89f), Size(56f, 51f))
  drawRect(Color(0xFFF4E6B6), Offset(100f, 91f), Size(63f, 31f))
  val roof =
    Path().apply {
      moveTo(14f, 56f)
      lineTo(39f, 20f)
      lineTo(163f, 20f)
      lineTo(188f, 56f)
      close()
    }
  drawPath(roof, Color(0xFF246376))
  repeat(
    7,
  ) { i ->
    drawRoundRect(
      if (i % 2 == 0)Color(0xFFF4D18B)else Color(0xFF337D8A),
      Offset(15f + i * 24f, 51f),
      Size(24f, 21f),
      CornerRadius(5f, 5f),
    )
  }
  drawRect(Color(0xFF347D75), Offset(108f, 97f), Size(12f, 18f))
  drawRect(Color(0xFFC17F3A), Offset(128f, 101f), Size(25f, 14f))
  drawLine(Color(0xFF4C7475), Offset(8f, 143f), Offset(193f, 143f), 6f)
}

@Composable
fun HeroNumber(
  label: String,
  value: String,
  modifier: Modifier = Modifier,
) {
  Column(modifier.padding(12.dp)) {
    Text(label, style = MaterialTheme.typography.bodyMedium)
    Text(value, fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold)
  }
}

@Composable
fun LivelihoodHeading(
  type: Livelihood,
  title: String,
  subtitle: String,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(24.dp)).padding(16.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(Modifier.weight(1f)) {
      Text(title, fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold)
      Text(subtitle, modifier = Modifier.padding(top = 8.dp))
    }
    LivelihoodArt(type, Modifier.width(115.dp).height(106.dp))
  }
}
