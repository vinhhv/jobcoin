package vinhhv.io.jobcoin
package repository

import java.util.concurrent.ConcurrentLinkedQueue

import cats.effect.IO
import cats.syntax.apply._
import vinhhv.io.jobcoin.models.AddressType
import vinhhv.io.jobcoin.models.FundType
import vinhhv.io.jobcoin.models.{Address, DepositLog, Funds}

final class HouseTransferQueueInMemory extends HouseTransferQueue {
  private val queue = new ConcurrentLinkedQueue[DepositLog]()

  val get: IO[Option[DepositLog]] = IO.suspend(queue.poll() match {
    case d @ DepositLog(_, _) => IO(println(s"Retrieved $d from queue")) *> IO.pure(Some(d))
    case _ => IO(println("Empty queue!")) *> IO.pure(None)
  })

  def add(depositAddress: Address[AddressType.Deposit], amount: Funds[FundType.Deposit]): IO[Unit] =
    IO(queue.add(DepositLog(depositAddress, amount)))
}
