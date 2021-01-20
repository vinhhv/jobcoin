package vinhhv.io.jobcoin.service

import cats.effect.IO
import cats.syntax.apply._
import vinhhv.io.jobcoin.models.AddressType._
import vinhhv.io.jobcoin.models.{Address, AddressInfo, Funds}
import vinhhv.io.jobcoin.repository.{CoinRepository, HouseTransferQueue, MixerRepository}

final class TransferService(repo: CoinRepository, mixerRepo: MixerRepository, queue: HouseTransferQueue) {
  def sendCoins(fromAddress: String, toAddress: String, deposit: Double): IO[Unit] =
    for {
      standardFromAddress <- IO.fromTry(Address.create[Standard](fromAddress))
      standardToAddress <- IO.fromTry(Address.create[Standard](toAddress))
      deposit <- IO.fromTry(Funds.createDeposit(deposit))
      _ <- repo.sendCoins(standardFromAddress, standardToAddress, deposit)
      isDepositAddress <- mixerRepo.isDepositAddress(toAddress)
      _ <-
        if (isDepositAddress) {
          IO(println(s"Adding deposit transaction to queue: ${toAddress} ${deposit.amount}")) *>
          IO.fromTry(Address.create[Deposit](toAddress)).flatMap(address => queue.add(address, deposit))
        } else {
          IO.unit
        }
    } yield ()

  def getAddressInfo(address: String): IO[AddressInfo] =
    for {
      address <- IO.fromTry(Address.create[Standard](address))
      json <- repo.getAddressInfo(address)
      addressInfo <- IO.fromTry(AddressInfo.fromJson(json))
    } yield addressInfo
}
