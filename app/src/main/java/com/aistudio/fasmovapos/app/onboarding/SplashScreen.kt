package com.aistudio.fasmovapos.app.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.aistudio.fasmovapos.app.R
import kotlinx.coroutines.delay

/**
 * Apenkoder Software splash screen - a single full-bleed brand image.
 *
 * Timing: 600ms fade in, ~1800ms hold, 600ms fade out (3s total). Calls [onFinish] once
 * the fade-out completes.
 */
@Composable
fun SplashScreen(
  onFinish: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val currentOnFinish by rememberUpdatedState(onFinish)
  val alpha = remember { Animatable(0f) }

  LaunchedEffect(Unit) {
    alpha.animateTo(1f, animationSpec = tween(durationMillis = 600))
    delay(1800)
    alpha.animateTo(0f, animationSpec = tween(durationMillis = 600))
    currentOnFinish()
  }

  Surface(modifier = modifier.fillMaxSize(), color = ApenkoderColors.Navy) {
    Image(
      painter = painterResource(id = R.drawable.apenkoder_splasher),
      contentDescription = "Apenkoder Software",
      contentScale = ContentScale.Crop,
      modifier = Modifier.fillMaxSize().alpha(alpha.value),
    )
  }
}
