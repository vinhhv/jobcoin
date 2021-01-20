package vinhhv.io.jobcoin.models

import vinhhv.io.jobcoin.models.AddressType._

final case class DistributionAddresses(
  houseAddress: Address[House],
  // TODO: Make non-empty list
  sinkAddresses: List[Address[Standard]]
)
