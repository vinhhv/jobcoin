package vinhhv.io.jobcoin.service

import cats.effect.{ContextShift, IO}
import cats.syntax.apply._
import fs2.Stream
import vinhhv.io.jobcoin.models.DepositLog
import vinhhv.io.jobcoin.repository.{HouseTransferQueue, MixerRepository}

final class HouseTransferService(repo: MixerRepository, queue: HouseTransferQueue) {
  final case class DepositLogIteration(count: Int, depositLog: Option[DepositLog])
  // Transfers balances from deposit addresses to their linked house addresses.
  def startTransfers(implicit cs: ContextShift[IO]): IO[Unit] = {
    for {
      _ <- Stream
        .unfoldEval[IO, DepositLogIteration, Int](DepositLogIteration(0, None)) {
          case DepositLogIteration(count, log) if log.isEmpty && count != 0 =>
            IO(println(s"Processed all ${count - 1} items in the queue.")) *> IO.pure(None)
          case DepositLogIteration(count, log) if log.isEmpty && count == 0 =>
            IO(println("Starting fiber to transfer funds from deposit addresses to house addresses")) *>
            queue.get.map(d => Some(count + 1 -> DepositLogIteration(count + 1, d)))
          case DepositLogIteration(count, Some(depositLog)) =>
            IO(println(s"Processing deposit from ${depositLog.address} with amount ${depositLog.amount}")) *>
            queue.get.map(d => Some(count + 1 -> DepositLogIteration(count + 1, d)))
        }
        .compile
        .drain
        .start
    } yield ()
  }
}
