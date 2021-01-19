package vinhhv.io.jobcoin
package service

import java.util.UUID

import cats.effect.IO
import cats.instances.list._
import cats.syntax.traverse._
import vinhhv.io.jobcoin.models.Address
import vinhhv.io.jobcoin.repository.MixerRepository

final class MixerService(repo: MixerRepository) {
  // Creates mixer addresses using the list of provided addresses
  def createMixerAddresses(addresses: List[String]): IO[Address.DepositAddress] = {
    for {
      _ <- if (addresses.isEmpty) IO.raiseError(EmptyAddressException()) else IO.unit
      addresses <- addresses.map(Address.createStandard).sequence
      depositAddress <- Address.createDeposit(generateRandomAddress)
      houseAddress <- Address.createHouse(generateRandomAddress)
      _ <- repo.createMixerPipeline(depositAddress, houseAddress, addresses)
    } yield depositAddress
  }

  def generateRandomAddress: String = UUID.randomUUID().toString

  // Main function to mix and distribute Jobcoins in existing Housing Accounts.
  def distributeJobcoin: IO[Unit] = {
    for {
      distributionAddresses <- repo.getDistributionAddresses
    } yield ()
  }
}
