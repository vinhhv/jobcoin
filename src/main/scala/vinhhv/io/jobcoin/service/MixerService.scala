package vinhhv.io.jobcoin.service

import java.util.UUID

import cats.effect.IO
import cats.instances.list._
import cats.syntax.traverse._
import vinhhv.io.jobcoin.models.Address
import vinhhv.io.jobcoin.repository.MixerRepository

final class MixerService(repo: MixerRepository) {
  def createMixerAddresses(addresses: List[String]): IO[Address.DepositAddress] = {
    for {
      addresses <- addresses.map(Address.createStandard).sequence
      depositAddress <- Address.createDeposit(generateRandomAddress)
      houseAddress <- Address.createHouse(generateRandomAddress)
      _ <- repo.createMixerPipeline(depositAddress, houseAddress, addresses)
    } yield depositAddress
  }

  def generateRandomAddress: String = UUID.randomUUID().toString
}
