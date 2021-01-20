package vinhhv.io.jobcoin.models

import io.circe.Json

import scala.util.{Failure, Try}

final case class AddressInfo(
  balance: Funds[FundType.Balance]
)
object AddressInfo {
  def fromJson(json: Json): Try[AddressInfo] = {
    val cursor = json.hcursor
    cursor.downField("balance").as[Double] match {
      case Left(error) => Failure(error)
      case Right(balance) => Funds.createBalance(balance).map(AddressInfo(_))
    }
  }
}
