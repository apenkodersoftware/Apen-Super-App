package com.aistudio.fasmovapos.app.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Lets one operation run at a time: while a call is in flight, further calls are dropped instead of queued.
 * Used so a double tap on a write control (checkout, save) cannot create the same record twice.
 */
class SingleFlight {
  private val inFlight = AtomicBoolean(false)
  private val _isRunning = MutableStateFlow(false)

  /** True while a call is in flight, so the UI can disable the control that started it. */
  val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

  /** Runs [block] unless another call is still running; returns false when the call was dropped. */
  suspend fun runIfIdle(block: suspend () -> Unit): Boolean {
    if (!inFlight.compareAndSet(false, true)) return false
    _isRunning.value = true
    try {
      block()
    } finally {
      _isRunning.value = false
      inFlight.set(false)
    }
    return true
  }
}
