package com.aistudio.fasmovapos.app.ui

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SingleFlightTest {
  @Test
  fun secondCallWhileFirstIsRunning_isDropped() =
    runTest {
      val flight = SingleFlight()
      val gate = CompletableDeferred<Unit>()
      var runs = 0
      var secondAccepted = true

      launch {
        flight.runIfIdle {
          runs++
          gate.await()
        }
      }
      runCurrent()
      launch { secondAccepted = flight.runIfIdle { runs++ } }
      runCurrent()
      gate.complete(Unit)
      runCurrent()

      assertFalse(secondAccepted)
      assertEquals(1, runs)
    }

  @Test
  fun callAfterCompletion_runsAgain() =
    runTest {
      val flight = SingleFlight()
      var runs = 0

      assertTrue(flight.runIfIdle { runs++ })
      assertTrue(flight.runIfIdle { runs++ })

      assertEquals(2, runs)
    }

  @Test
  fun failedCall_releasesTheGuard() =
    runTest {
      val flight = SingleFlight()

      runCatching { flight.runIfIdle { error("boom") } }

      assertTrue(flight.runIfIdle { })
    }
}
