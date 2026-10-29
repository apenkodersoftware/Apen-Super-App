package com.aistudio.fasmovapos.app

import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.platform.app.InstrumentationRegistry

/** Select an actual Android document row; no repository restore call or synthetic result URI. */
internal fun nativeAndroidText(
  text: String,
  document: Boolean = false,
) {
  val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
  automation.serviceInfo =
    automation.serviceInfo.apply {
      flags = flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
    }
  val until = SystemClock.uptimeMillis() + 90_000
  var seen = ""
  while (SystemClock.uptimeMillis() < until) {
    val roots = (listOfNotNull(automation.rootInActiveWindow) + automation.windows.mapNotNull { it.root })

    fun find(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
      if (node == null) return null
      if (node.text?.toString()?.equals(text, ignoreCase = true) == true ||
        node.contentDescription?.toString()?.equals(text, ignoreCase = true) == true
      ) {
        return node
      }
      for (i in 0 until node.childCount) find(node.getChild(i))?.let { return it }
      return null
    }
    seen = roots.joinToString { it.packageName.toString() + ":" + it.childCount }
    var match = roots.firstNotNullOfOrNull(::find)
    while (match != null) {
      if (!document && match.isClickable && match.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return
      val bounds = android.graphics.Rect().also { match!!.getBoundsInScreen(it) }
      if (match.text?.toString()?.equals(text, ignoreCase = true) == true && !bounds.isEmpty) {
        // Some DocumentsUI rows do not expose ACTION_CLICK, while the text supplies exact observed bounds.
        val now = SystemClock.uptimeMillis()
        val x = bounds.exactCenterX()
        val y = bounds.exactCenterY()
        val down = android.view.MotionEvent.obtain(now, now, android.view.MotionEvent.ACTION_DOWN, x, y, 0)
        down.source = android.view.InputDevice.SOURCE_TOUCHSCREEN
        try {
          check(automation.injectInputEvent(down, true))
          SystemClock.sleep(if (document) 650 else 100)
          val up = android.view.MotionEvent.obtain(now, SystemClock.uptimeMillis(), android.view.MotionEvent.ACTION_UP, x, y, 0)
          up.source = android.view.InputDevice.SOURCE_TOUCHSCREEN
          try {
            check(automation.injectInputEvent(up, true))
          } finally {
            up.recycle()
          }
        } finally {
          down.recycle()
        }
        if (document) nativeAndroidText("Open")
        return
      }
      match = match.parent
    }
    SystemClock.sleep(500)
  }
  error("Document row not found: $text; roots=$seen")
}
