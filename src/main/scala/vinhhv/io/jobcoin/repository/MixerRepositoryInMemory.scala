package vinhhv.io.jobcoin
package repository

import java.util.concurrent.ConcurrentHashMap

import cats.Apply.ops._
import cats.effect.IO
import vinhhv.io.jobcoin.models.Address
import vinhhv.io.jobcoin.models.Address.{DepositAddress, HouseAddress, StandardAddress}

// For the purpose of simplicity, we will link our deposit, housing
// and sink addresses using two concurrent hash maps. They will NOT
// outlive the life of the server.
final class MixerRepositoryInMemory extends MixerRepository {
  val depositToHousingMap = new ConcurrentHashMap[DepositAddress, HouseAddress]()
  val housingToStandardMap = new ConcurrentHashMap[HouseAddress, Set[StandardAddress]]()

  def createMixerPipeline(
      depositAddress: Address.DepositAddress,
      houseAddress: Address.HouseAddress,
      sinkAddresses: List[Address.StandardAddress]
  ): IO[Unit] = {
    (depositToHousingMap.contains(depositAddress), housingToStandardMap.contains(houseAddress)) match {
      case (true, _) => IO.raiseError(DepositAddressAlreadyInUseException(depositAddress.name))
      case (_, true) => IO.raiseError(HouseAddressAlreadyInUseException(houseAddress.name))
      case (false, false) =>
        IO(depositToHousingMap.put(depositAddress, houseAddress)) *>
        IO(housingToStandardMap.put(houseAddress, sinkAddresses.toSet)) *>
        IO(println(depositToHousingMap)) *> IO(println(housingToStandardMap))
    }
  }

}
