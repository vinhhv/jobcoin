package vinhhv.io.jobcoin
package repository

import java.util.concurrent.ConcurrentHashMap

import cats.effect.IO
import cats.syntax.apply._
import vinhhv.io.jobcoin.models.{Address, DistributionAddresses}
import vinhhv.io.jobcoin.models.Address.{DepositAddress, HouseAddress, StandardAddress}

import scala.collection.JavaConverters._

// For the purpose of simplicity, we will link our deposit, housing
// and sink addresses using two concurrent hash maps. They will NOT
// outlive the life of the server.
final class MixerRepositoryInMemory extends MixerRepository {
  private val depositToHousingMap = new ConcurrentHashMap[DepositAddress, HouseAddress]()
  private val housingToSinkMap = new ConcurrentHashMap[HouseAddress, Set[StandardAddress]]()

  def createMixerPipeline(
      depositAddress: Address.DepositAddress,
      houseAddress: Address.HouseAddress,
      sinkAddresses: List[Address.StandardAddress]
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
            IO(housingToSinkMap.put(houseAddress, sinkAddresses.toSet)) *>
            IO(println(depositToHousingMap)) *> IO(println(housingToSinkMap))
      }
    } yield ()
  }

  def getDistributionAddresses: IO[List[DistributionAddresses]] =
    IO(housingToSinkMap.asScala.toList.map {
      case (houseAddress, sinkAddresses) => DistributionAddresses(houseAddress, sinkAddresses.toList)
    })

  def isDepositAddress(name: String): IO[Boolean] =
    for {
      depositAddress <- Address.createDeposit(name)
      isDepositAddress <- IO(depositToHousingMap.containsKey(depositAddress))
    } yield isDepositAddress

  def getHouseAddress(depositAddress: DepositAddress): IO[HouseAddress] =
    for {
      houseAddress <-
        if (depositToHousingMap.containsKey(depositAddress))
          IO(depositToHousingMap.get(depositAddress))
        else
          IO.raiseError(JobCoinServerError(s"Deposit address ${depositAddress.name} has no housing address"))
    } yield houseAddress
}
