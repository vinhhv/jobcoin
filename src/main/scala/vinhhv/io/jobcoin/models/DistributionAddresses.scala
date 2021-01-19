package vinhhv.io.jobcoin.models

final case class DistributionAddresses(
  houseAddress: Address.HouseAddress,
  sinkAddresses: List[Address.StandardAddress]
)
