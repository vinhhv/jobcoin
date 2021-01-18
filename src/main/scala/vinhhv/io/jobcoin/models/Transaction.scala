package vinhhv.io.jobcoin.models

import java.sql.Timestamp

// Not really needed for our current exercise
final case class Transaction(
  timestamp: Timestamp,
  fromAddress: Option[Address],
  toAddress: Address
)
