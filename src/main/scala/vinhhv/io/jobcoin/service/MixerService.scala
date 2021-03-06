package vinhhv.io.jobcoin
package service

import java.util.UUID

import cats.effect.{ContextShift, IO}
import cats.instances.list._
import cats.syntax.apply._
import cats.syntax.parallel._
import cats.syntax.traverse._
import com.typesafe.scalalogging.LazyLogging
import vinhhv.io.jobcoin.Settings.PRECISION
import vinhhv.io.jobcoin.algorithm.RandomDistribution
import vinhhv.io.jobcoin.models.AddressType
import vinhhv.io.jobcoin.models.FundType
import vinhhv.io.jobcoin.models.{Address, DistributionAddresses, Funds}
import vinhhv.io.jobcoin.repository.MixerRepository

final class MixerService(repo: MixerRepository, transferService: TransferService) {
  // Creates mixer addresses using the list of provided addresses
  def createMixerAddresses(addresses: List[String]): IO[Address[AddressType.Deposit]] = {
    for {
      _ <- if (addresses.isEmpty) IO.raiseError(EmptyAddressException()) else IO.unit
      // Change to traverse
      addresses <- addresses.traverse(address => IO.fromTry(Address.create[AddressType.Standard](address)))
      depositAddress <- IO.fromTry(Address.create[AddressType.Deposit](generateRandomAddress))
      houseAddress <- IO.fromTry(Address.create[AddressType.House](generateRandomAddress))
      _ <- repo.createMixerPipeline(depositAddress, houseAddress, addresses)
    } yield depositAddress
  }

  def generateRandomAddress: String = UUID.randomUUID().toString

  // Main function to mix and distribute Jobcoins in existing Housing Accounts.
  def distributeJobcoin(implicit cs: ContextShift[IO]): IO[Unit] = {
    for {
      distributionAddresses <- repo.getDistributionAddresses
      addressBalances <- MixerService.getAllAddressBalancesPar(distributionAddresses, transferService)
      distributions <- MixerService.calculateDistributions(addressBalances)
      _ <- MixerService.sendDistributions(distributions, transferService)
    } yield ()
  }
}
object MixerService extends LazyLogging {
  type Distribution = (Address[AddressType.Standard], Double)

  final case class MixerAddressInfo(
    houseAddress: Address[AddressType.House],
    sinkAddresses: List[Address[AddressType.Standard]],
    balance: Funds[FundType.Balance])

  final case class MixerAddressDistribution(
    houseAddress: Address[AddressType.House],
    distributions: List[Distribution])

  def getAllAddressBalancesPar(
      addresses: List[DistributionAddresses],
      transferService: TransferService
  )(implicit cs: ContextShift[IO]): IO[List[MixerAddressInfo]] =
    addresses
      .parTraverse {
        distributionAddress =>
          transferService
            .getAddressInfo(distributionAddress.houseAddress.name)
            .map(addressInfo =>
              MixerAddressInfo(distributionAddress.houseAddress, distributionAddress.sinkAddresses, addressInfo.balance)
            )
      }
      .map(infos => infos.filter(info => info.balance.amount > 0.toDouble))

  def calculateDistributions(mixerAddressInfos: List[MixerAddressInfo]): IO[List[MixerAddressDistribution]] =
    mixerAddressInfos.traverse { info =>
      val totalBalance = info.balance.amount
      val numberSinks = info.sinkAddresses.length
      RandomDistribution
        .getPercentageDistribution(info.balance.amount, info.sinkAddresses.length)
        .flatMap { percentageShouldDistribute =>
          val totalShouldDistribute =
            BigDecimal(totalBalance * percentageShouldDistribute)
              .setScale(PRECISION, BigDecimal.RoundingMode.HALF_UP)
              .toDouble
          RandomDistribution
            .randomDistribution(totalShouldDistribute, numberSinks)
            .map { distributions =>
              MixerAddressDistribution(info.houseAddress, info.sinkAddresses.zip(distributions))
            }
        }
    }

  def sendDistributions(
      distributions: List[MixerAddressDistribution],
      transferService: TransferService
  )(implicit cs: ContextShift[IO]): IO[List[Unit]] =
    distributions
      .flatMap { distribution =>
        distribution.distributions.map {
          case (sinkAddress, amount) =>
            IO(logger.info(s"Distributing $amount from ${distribution.houseAddress} to $sinkAddress\n")) *>
            transferService.sendCoins(distribution.houseAddress.name, sinkAddress.name, amount)
        }
      }
      .parSequence
}
