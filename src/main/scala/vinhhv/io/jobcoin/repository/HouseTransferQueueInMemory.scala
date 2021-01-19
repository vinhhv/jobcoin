package vinhhv.io.jobcoin
package repository

import java.util.concurrent.ConcurrentLinkedQueue

import cats.effect.IO
import cats.syntax.apply._
import vinhhv.io.jobcoin.models.{Address, DepositLog, Funds}

final class HouseTransferQueueInMemory extends HouseTransferQueue {
  private val queue = new ConcurrentLinkedQueue[DepositLog]()

  def get: IO[Option[DepositLog]] = queue.poll() match {
    case d @ DepositLog(_, _) => IO(println("Retrieved $d from queue")) *> IO.pure(Some(d))
    case _ => IO(println("Empty queue!")) *> IO.pure(None)
  }

  def add(depositAddress: Address.DepositAddress, amount: Funds.Deposit): IO[Unit] =
    IO(queue.add(DepositLog(depositAddress, amount)))
}
