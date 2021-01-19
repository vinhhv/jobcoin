package vinhhv.io.jobcoin.repository

import cats.effect.IO
import vinhhv.io.jobcoin.models.Address.{DepositAddress, HouseAddress, StandardAddress}

trait MixerRepository {
  // Links the following addresses via some persistence strategy.
  // ex. Deposit address 'DepA' is linked to housing address 'HouseA'
  // which is the source for small incremental deposits into sink addresses
  // 'SinkA', 'SinkB', ..., and so on.
  def createMixerPipeline(
    depositAddress: DepositAddress,
    houseAddress: HouseAddress,
    sinkAddresses: List[StandardAddress]): IO[Unit]

  def isDepositAddress(name: String): IO[Boolean]
}
