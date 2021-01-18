package vinhhv.io.jobcoin.models

import cats.effect.IO
import io.circe.Json

final case class AddressInfo(
  balance: Funds.Balance
  // Not really needed for our current exercise
  // transactions: List[Transaction]
)
object AddressInfo {
  def fromJson(json: Json): IO[AddressInfo] = {
    val cursor = json.hcursor
    cursor.downField("balance").as[Double] match {
      case Left(error) => IO.raiseError(error)
      case Right(balance) => Funds.createBalance(balance).map(AddressInfo(_))
    }
  }
}
