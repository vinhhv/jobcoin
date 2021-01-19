package vinhhv.io.jobcoin
package service

import java.util.UUID

import cats.effect.{ContextShift, IO}
import cats.instances.list._
import cats.syntax.apply._
import cats.syntax.parallel._
import cats.syntax.traverse._
import vinhhv.io.jobcoin.algorithm.RandomDistribution
import vinhhv.io.jobcoin.models.{Address, DistributionAddresses, Funds}
import vinhhv.io.jobcoin.repository.MixerRepository

final class MixerService(repo: MixerRepository, transferService: TransferService) {
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
  def distributeJobcoin(implicit cs: ContextShift[IO]): IO[Unit] = {
    for {
      distributionAddresses <- repo.getDistributionAddresses
      addressBalances <- MixerService.getAllAddressBalancesPar(distributionAddresses, transferService)
      distributions = MixerService.calculateDistributions(addressBalances)
      _ <- MixerService.sendDistributions(distributions, transferService)
    } yield ()
  }
}
object MixerService {
  type Distribution = (Address.StandardAddress, Double)

  final case class MixerAddressInfo(
    houseAddress: Address.HouseAddress,
    sinkAddresses: List[Address.StandardAddress],
    balance: Funds.Balance)

  final case class MixerAddressDistribution(
    houseAddress: Address.HouseAddress,
    distributions: List[Distribution])

  def getAllAddressBalancesPar(
      addresses: List[DistributionAddresses],
      transferService: TransferService
  )(implicit cs: ContextShift[IO]): IO[List[MixerAddressInfo]] =
    addresses
      .map {
        distributionAddress =>
          transferService
            .getAddressInfo(distributionAddress.houseAddress.name)
            .map(addressInfo =>
              MixerAddressInfo(distributionAddress.houseAddress, distributionAddress.sinkAddresses, addressInfo.balance)
            )
      }
      .parSequence

  def calculateDistributions(mixerAddressInfos: List[MixerAddressInfo]): List[MixerAddressDistribution] =
    mixerAddressInfos.map { info =>
      val totalBalance = info.balance.amount
      val numberSinks = info.sinkAddresses.length
      val percentageShouldDistribute =
        RandomDistribution.getPercentageDistribution(info.balance.amount, info.sinkAddresses.length)

      val totalShouldDistribute = totalBalance * percentageShouldDistribute
      val distributions = RandomDistribution.randomDistribution(totalShouldDistribute, numberSinks)
      MixerAddressDistribution(info.houseAddress, info.sinkAddresses.zip(distributions))
    }

  def sendDistributions(
      distributions: List[MixerAddressDistribution],
      transferService: TransferService
  )(implicit cs: ContextShift[IO]): IO[List[Unit]] =
    distributions
      .flatMap { distribution =>
        distribution.distributions.map {
          case (sinkAddress, amount) =>
            IO(println(s"Distributing $amount from ${distribution.houseAddress} to $sinkAddress")) *>
            transferService.sendCoins(distribution.houseAddress.name, sinkAddress.name, amount)
        }
      }
      .parSequence
}
