package ziocircuitbreaker

import java.util.concurrent.TimeUnit

import scala.concurrent.duration._
import zio.clock.Clock
import zio.duration.Duration
import zio.{RefM, UIO, URIO, ZIO, clock}

/**
  * Circuit Breaker definition.
  *
  * @param failures list of timestamps in milliseconds when failures occurred
  * @param successes list of timestamps in milliseconds when successes occurred
  * @param timeoutToRef time in milliseconds when circuit breaker will become available,
  *                      if less than current time then it is available
  * @param timeout time to wait when error ratio reaches @ratio
  * @param resetTimeout aggregation time
  * @param ratio value between 0 and 1, percent of failed actions to start using @timeout
  * @param handleErrorsFrom how many @failures + @successes should be before using @timeout
  */
class CircuitBreaker private (val failures: RefM[Vector[Long]],
                              val successes: RefM[Vector[Long]],
                              val timeoutToRef: RefM[Long],
                              val timeout: FiniteDuration,
                              val resetTimeout: FiniteDuration,
                              val ratio: Double,
                              val handleErrorsFrom: Int) {
  def withCB[R, E, A](action: ZIO[R, E, A]): ZIO[R with Clock, E, A] =
    for {
      now <- currentTimeMs
      timeoutTo <- timeoutToRef.get
      _ <- if (now < timeoutTo)
        clock.sleep(Duration.fromScala((timeoutTo - now).millis))
      else ZIO.unit
      res <- action.tapBoth(_ => addFailure.flatMap(errorHandler),
                            _ => addSuccess)
    } yield res

  private def errorHandler(failuresSize: Int): URIO[Clock, Unit] = {
    for {
      succs <- successes.get
      bothSize = succs.size + failuresSize
      newRatio = failuresSize.toDouble / bothSize
      _ <- if (newRatio >= ratio && bothSize >= handleErrorsFrom)
        timeoutToRef.update(_ => currentTimeMs.map(_ + timeout.toMillis))
      else ZIO.unit
    } yield ()
  }

  private def addSuccess: URIO[Clock, Unit] =
    pushPopTime(successes).unit

  private def addFailure: URIO[Clock, Int] =
    pushPopTime(failures)

  private def pushPopTime(ref: RefM[Vector[Long]]): URIO[Clock, Int] =
    ref.modify { curr =>
      currentTimeMs.map { ct =>
        val res = curr.dropWhile(_ < ct - resetTimeout.toMillis) :+ ct
        (res.size, res)
      }
    }

  private def currentTimeMs: ZIO[Clock, Nothing, Long] =
    clock.currentTime(TimeUnit.MILLISECONDS)
}

object CircuitBreaker {
  def make(timeout: FiniteDuration,
           resetTimeout: FiniteDuration,
           ratio: Double,
           handleErrorsFrom: Int = 2): UIO[CircuitBreaker] =
    for {
      fails <- RefM.make(Vector.empty[Long])
      succs <- RefM.make(Vector.empty[Long])
      timeoutToRef <- RefM.make(0L)
    } yield
      new CircuitBreaker(fails,
                         succs,
                         timeoutToRef,
                         timeout,
                         resetTimeout,
                         ratio,
                         handleErrorsFrom)
}
