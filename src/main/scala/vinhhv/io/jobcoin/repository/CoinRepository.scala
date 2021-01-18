package vinhhv.io.jobcoin.repository

import cats.effect.IO
import io.circe.Json
import vinhhv.io.jobcoin.models.Address
import vinhhv.io.jobcoin.models.Funds.Deposit

trait CoinRepository {
  def sendCoins(fromAddress: Address, toAddress: Address, deposit: Deposit): IO[Unit]
  def getAddressInfo(address: Address): IO[Json]
}
