package vinhhv.io.jobcoin.service

import cats.effect.{ContextShift, IO}
import cats.syntax.apply._
import fs2.Stream
import vinhhv.io.jobcoin.models.AddressType.Deposit
import vinhhv.io.jobcoin.models.{Address, DepositLog}
import vinhhv.io.jobcoin.repository.{HouseTransferQueue, MixerRepository}

final class HouseTransferService(transferService: TransferService, repo: MixerRepository, queue: HouseTransferQueue) {
  final case class DepositLogIteration(count: Int, depositLog: Option[DepositLog])
  // Transfers balances from deposit addresses to their linked house addresses.
  def startTransfers(implicit cs: ContextShift[IO]): IO[Unit] = {
    for {
      _ <- Stream
        .unfoldEval[IO, DepositLogIteration, Int](DepositLogIteration(0, None)) {
          case DepositLogIteration(count, log) if log.isEmpty && count != 0 => IO.pure(None)
          case DepositLogIteration(count, log) if log.isEmpty && count == 0 =>
            queue.get.map(d => Some(count + 1 -> DepositLogIteration(count + 1, d)))
          case DepositLogIteration(count, Some(depositLog)) =>
            for {
              depositAddress <- IO.fromTry(Address.create[Deposit](depositLog.address.name))
              houseAddress <- repo.getHouseAddress(depositAddress)
              _ <- IO(
                println(s"Processing deposit from ${depositLog.address} with amount ${depositLog.amount} to $houseAddress\n"))
              _ <- transferService.sendCoins(depositAddress.name, houseAddress.name, depositLog.amount.amount)
              nextLog <- queue.get.map(d => Some(count + 1 -> DepositLogIteration(count + 1, d)))
            } yield nextLog
        }
        .compile
        .drain
        .start
    } yield ()
  }
}
