package vinhhv.io.jobcoin
package repository

import cats.effect.IO
import io.circe.Json
import io.circe.parser.parse
import sttp.client3._
import vinhhv.io.jobcoin.models.{Address, Funds}

final class JobCoinAPI extends CoinRepository {
  val backend = HttpURLConnectionBackend()

  def sendCoins(fromAddress: Address, toAddress: Address, deposit: Funds.Deposit): IO[Unit] = {
    val uri = uri"http://jobcoin.gemini.com/parole-structure/api/transactions"
    val request = basicRequest
      .post(uri)
      .body(Map("fromAddress" -> fromAddress.name, "toAddress" -> toAddress.name, "amount" -> deposit.amount.toString))
    val responseIO = IO(request.send(backend))
    responseIO.flatMap { response =>
      response.isSuccess match {
        case false => IO.raiseError(InsufficientFundsException(fromAddress.name))
        case true => IO.unit
      }
    }
  }

  def getAddressInfo(address: Address): IO[Json] = {
    val uri = uri"http://jobcoin.gemini.com/parole-structure/api/addresses/${address.name}"
    val request = basicRequest.get(uri)
    val responseIO = IO(request.send(backend))
    responseIO.flatMap { response =>
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
