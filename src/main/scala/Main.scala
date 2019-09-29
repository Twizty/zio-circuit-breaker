import java.util.concurrent.TimeUnit

import zio.{IO, ZIO, clock, console}
import ziocircuitbreaker.CircuitBreaker

import scala.concurrent.duration._

object Main extends zio.App {
  override def run(args: List[String]): ZIO[Main.Environment, Nothing, Int] = {
    for {
      ts1 <- clock.currentTime(TimeUnit.MILLISECONDS)
      cb <- CircuitBreaker.make(5.seconds, 20.seconds, 0.5, 3)
      _ <- cb.withCB(fail[Int]("1")).either
      ts2 <- clock.currentTime(TimeUnit.MILLISECONDS)
      _ <- cb.withCB(fail[Int]("1")).either
      ts3 <- clock.currentTime(TimeUnit.MILLISECONDS)
      _ <- cb.withCB(fail[Int]("1")).either
      ts4 <- clock.currentTime(TimeUnit.MILLISECONDS)
      _ <- cb.withCB(ZIO.unit)
      ts5 <- clock.currentTime(TimeUnit.MILLISECONDS)
      _ <- cb.withCB(ZIO.unit)
      ts6 <- clock.currentTime(TimeUnit.MILLISECONDS)
      _ <- console.putStrLn(s"t2 - t1 = ${ts2 - ts1}")
      _ <- console.putStrLn(s"t3 - t2 = ${ts3 - ts2}")
      _ <- console.putStrLn(s"t4 - t3 = ${ts4 - ts3}")
      _ <- console.putStrLn(s"t5 - t4 = ${ts5 - ts4}")
      _ <- console.putStrLn(s"t6 - t5 = ${ts6 - ts5}")
    } yield 0
  }

  def fail[A](msg: String): IO[Throwable, A] = ZIO.fail(new Throwable(msg))
}
