package vinhhv.io.jobcoin
package repository

import java.util.concurrent.ConcurrentHashMap

import cats.effect.IO
import cats.syntax.apply._
import vinhhv.io.jobcoin.models.AddressType._
import vinhhv.io.jobcoin.models.{Address, DistributionAddresses}

import scala.collection.JavaConverters._

// For the purpose of simplicity, we will link our deposit, housing
// and sink addresses using two concurrent hash maps. They will NOT
// outlive the life of the server.
final class MixerRepositoryInMemory extends MixerRepository {
  private val depositToHousingMap = new ConcurrentHashMap[Address[Deposit], Address[House]]()
  private val housingToSinkMap = new ConcurrentHashMap[Address[House], Set[Address[Standard]]]()

  def createMixerPipeline(
      depositAddress: Address[Deposit],
      houseAddress: Address[House],
      sinkAddresses: List[Address[Standard]]
  ): IO[Unit] = {
    for {
      _ <- IO(println(s"Creating mixer pipeline for ${depositAddress.name} -> ${houseAddress.name} -> $sinkAddresses"))
      containsDepositAddress <- IO(depositToHousingMap.contains(depositAddress))
      containsHouseAddress <- IO(housingToSinkMap.contains(houseAddress))
      _ <- (containsDepositAddress, containsHouseAddress) match {
        case (true, _) => IO.raiseError(DepositAddressAlreadyInUseException(depositAddress.name))
        case (_, true) => IO.raiseError(HouseAddressAlreadyInUseException(houseAddress.name))
        case (false, false) =>
          IO(depositToHousingMap.put(depositAddress, houseAddress)) *>
            IO(housingToSinkMap.put(houseAddress, sinkAddresses.toSet))
      }
    } yield ()
  }

  def getDistributionAddresses: IO[List[DistributionAddresses]] = {
    IO(housingToSinkMap.asScala.toList.map {
      case (houseAddress, sinkAddresses) => DistributionAddresses(houseAddress, sinkAddresses.toList)
    })
  }

  def isDepositAddress(name: String): IO[Boolean] =
    for {
      depositAddress <- IO.fromTry(Address.create[Deposit](name))
      isDepositAddress <- IO(depositToHousingMap.containsKey(depositAddress))
    } yield isDepositAddress

  def getHouseAddress(depositAddress: Address[Deposit]): IO[Address[House]] =
    for {
      houseAddress <-
        if (depositToHousingMap.containsKey(depositAddress))
          IO(depositToHousingMap.get(depositAddress))
        else
          IO.raiseError(JobCoinServerError(s"Deposit address ${depositAddress.name} has no housing address"))
    } yield houseAddress
}
