package vinhhv.io.jobcoin.repository

import cats.effect.IO
import vinhhv.io.jobcoin.models.{Address, AddressType, DepositLog, FundType, Funds}

trait HouseTransferQueue {
  def get: IO[Option[DepositLog]]
  def add(depositAddress: Address[AddressType.Deposit], amount: Funds[FundType.Deposit]): IO[Unit]
}