package vinhhv.io.jobcoin
package repository

import cats.effect.IO
import io.circe.Json
import io.circe.parser.parse
import sttp.client3._
import vinhhv.io.jobcoin.Settings.{ADDRESSES_URL, TRANSACTIONS_URL}
import vinhhv.io.jobcoin.models.{Address, FundType, Funds}

final class JobCoinAPI(backend: SttpBackend[IO, Any]) extends CoinRepository {
  def sendCoins(fromAddress: Address[_], toAddress: Address[_], deposit: Funds[FundType.Deposit]): IO[Unit] = {
    val uri = uri"${TRANSACTIONS_URL}"
    val request = basicRequest
      .post(uri)
      .body(Map("fromAddress" -> fromAddress.name, "toAddress" -> toAddress.name, "amount" -> deposit.amount.toString))
    request.send(backend).flatMap { response =>
      response.isSuccess match {
        case false => IO.raiseError(InsufficientFundsException(fromAddress.name))
        case true => IO.unit
      }
    }
  }

  def getAddressInfo(address: Address[_]): IO[Json] = {
    val uri = uri"${ADDRESSES_URL}/${address.name}"
    val request = basicRequest.get(uri)
    request.send(backend).flatMap { response =>
      response.body match {
        case Left(errorMessage: String) => IO.raiseError(JobCoinAPIException(response.code.code, errorMessage))
        case Right(jsonBody: String) => parse(jsonBody) match {
          case Left(parsingFailure) => IO.raiseError(parsingFailure)
          case Right(json) => IO.pure(json)
        }
      }
    }
  }
}
