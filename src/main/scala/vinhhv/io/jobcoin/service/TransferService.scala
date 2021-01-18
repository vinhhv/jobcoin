package vinhhv.io.jobcoin.service

import cats.effect.IO
import vinhhv.io.jobcoin.models.{Address, AddressInfo, Funds}
import vinhhv.io.jobcoin.repository.CoinRepository

final class TransferService(repo: CoinRepository) {
  def sendCoins(fromAddress: String, toAddress: String, deposit: Double): IO[Unit] =
    for {
      fromAddress <- Address.createStandard(fromAddress)
      toAddress <- Address.createStandard(toAddress)
      deposit <- Funds.createDeposit(deposit)
      _ <- repo.sendCoins(fromAddress, toAddress, deposit)
    } yield ()

  def getAddressInfo(address: String): IO[AddressInfo] =
    for {
      address <- Address.createStandard(address)
      json <- repo.getAddressInfo(address)
      addressInfo <- AddressInfo.fromJson(json)
    } yield addressInfo
}
