package vinhhv.io.jobcoin.models

final case class DepositLog(address: Address[AddressType.Deposit], amount: Funds[FundType.Deposit])
