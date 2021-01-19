package vinhhv.io.jobcoin.repository

import cats.effect.IO
import vinhhv.io.jobcoin.models.{Address, DepositLog, Funds}

trait HouseTransferQueue {
  def get: IO[Option[DepositLog]]
  def add(depositAddress: Address.DepositAddress, amount: Funds.Deposit): IO[Unit]
}