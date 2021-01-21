package vinhhv.io.jobcoin
package repository

import java.util.concurrent.ConcurrentLinkedQueue

import cats.effect.{Blocker, ContextShift, IO}
import cats.syntax.apply._
import vinhhv.io.jobcoin.models.AddressType
import vinhhv.io.jobcoin.models.FundType
import vinhhv.io.jobcoin.models.{Address, DepositLog, Funds}

final class HouseTransferQueueInMemory extends HouseTransferQueue {
  private val queue = new ConcurrentLinkedQueue[DepositLog]()

  def get(implicit cs: ContextShift[IO]): IO[Option[DepositLog]] = Blocker[IO].use { blocker =>
    blocker.blockOn(IO.suspend {
      queue.poll() match {
        case d @ DepositLog(_, _) => IO.pure(Some(d))
        case _ => IO.pure(None)
      }
    })
  }

  def add(depositAddress: Address[AddressType.Deposit], amount: Funds[FundType.Deposit]): IO[Unit] =
    IO(queue.add(DepositLog(depositAddress, amount)))
}
