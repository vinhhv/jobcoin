package vinhhv.io.jobcoin.service

import cats.effect.IO
import cats.syntax.apply._
import vinhhv.io.jobcoin.models.{Address, AddressInfo, Funds}
import vinhhv.io.jobcoin.repository.{CoinRepository, HouseTransferQueue, MixerRepository}

final class TransferService(repo: CoinRepository, mixerRepo: MixerRepository, queue: HouseTransferQueue) {
  def sendCoins(fromAddress: String, toAddress: String, deposit: Double): IO[Unit] =
    for {
      standardFromAddress <- Address.createStandard(fromAddress)
      standardToAddress <- Address.createStandard(toAddress)
      deposit <- Funds.createDeposit(deposit)
      _ <- repo.sendCoins(standardFromAddress, standardToAddress, deposit)
      isDepositAddress <- mixerRepo.isDepositAddress(toAddress)
      _ <-
        if (isDepositAddress) {
          IO(println(s"Adding deposit transaction to queue: ${toAddress} ${deposit.amount}")) *>
          Address.createDeposit(toAddress)
            .flatMap(address => queue.add(address, deposit))
        } else {
          IO.unit
        }
    } yield ()

  def getAddressInfo(address: String): IO[AddressInfo] =
    for {
      address <- Address.createStandard(address)
      json <- repo.getAddressInfo(address)
      addressInfo <- AddressInfo.fromJson(json)
    } yield addressInfo
}
