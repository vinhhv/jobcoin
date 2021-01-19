package vinhhv.io.jobcoin.repository

import cats.effect.IO
import vinhhv.io.jobcoin.models.Address.{DepositAddress, HouseAddress, StandardAddress}
import vinhhv.io.jobcoin.models.DistributionAddresses

trait MixerRepository {
  // Links the following addresses via some persistence strategy.
  // ex. Deposit address 'DepA' is linked to housing address 'HouseA'
  // which is the source for small incremental deposits into sink addresses
  // 'SinkA', 'SinkB', ..., and so on.
  def createMixerPipeline(
    depositAddress: DepositAddress,
    houseAddress: HouseAddress,
    sinkAddresses: List[StandardAddress]): IO[Unit]

  // Retrieves the housing address and sink addresses to run the mixer distribution.
  def getDistributionAddresses: IO[List[DistributionAddresses]]

  def isDepositAddress(name: String): IO[Boolean]

  def getHouseAddress(depositAddress: DepositAddress): IO[HouseAddress]
}
