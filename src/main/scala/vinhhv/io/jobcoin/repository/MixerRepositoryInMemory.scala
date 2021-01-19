package vinhhv.io.jobcoin
package repository

import java.util.concurrent.ConcurrentHashMap

import cats.effect.IO
import cats.syntax.apply._
import vinhhv.io.jobcoin.models.Address
import vinhhv.io.jobcoin.models.Address.{DepositAddress, HouseAddress, StandardAddress}

// For the purpose of simplicity, we will link our deposit, housing
// and sink addresses using two concurrent hash maps. They will NOT
// outlive the life of the server.
final class MixerRepositoryInMemory extends MixerRepository {
  private val depositToHousingMap = new ConcurrentHashMap[DepositAddress, HouseAddress]()
  private val housingToStandardMap = new ConcurrentHashMap[HouseAddress, Set[StandardAddress]]()

  def createMixerPipeline(
      depositAddress: Address.DepositAddress,
      houseAddress: Address.HouseAddress,
      sinkAddresses: List[Address.StandardAddress]
  ): IO[Unit] = {
    for {
      _ <- IO(println(s"Creating mixer pipeline for ${depositAddress.name} -> ${houseAddress.name} -> $sinkAddresses"))
      containsDepositAddress <- IO(depositToHousingMap.contains(depositAddress))
      containsHouseAddress <- IO(housingToStandardMap.contains(houseAddress))
      _ <- (containsDepositAddress, containsHouseAddress) match {
        case (true, _) => IO.raiseError(DepositAddressAlreadyInUseException(depositAddress.name))
        case (_, true) => IO.raiseError(HouseAddressAlreadyInUseException(houseAddress.name))
        case (false, false) =>
          IO(depositToHousingMap.put(depositAddress, houseAddress)) *>
            IO(housingToStandardMap.put(houseAddress, sinkAddresses.toSet)) *>
            IO(println(depositToHousingMap)) *> IO(println(housingToStandardMap))
      }
    } yield ()
  }

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
