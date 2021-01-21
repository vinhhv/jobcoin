package vinhhv.io.jobcoin

import cats.effect.IO
import io.finch.circe._
import io.finch.{BadRequest, Endpoint, InternalServerError, Ok}
import io.finch.catsEffect.{get, jsonBody, path, post}
import vinhhv.io.jobcoin.API._
import vinhhv.io.jobcoin.service.{MixerService, TransferService}

object Endpoints {
  def getAddressBalance(transferService: TransferService): Endpoint[IO, BalanceResponse] =
    get("balance" :: path[String]) { name: String =>
      transferService
        .getAddressInfo(name)
        .map(addressInfo => Ok(BalanceResponse(addressInfo.balance.amount.toString)))
    } handle {
      // TODO: This always returns server error no matter what, not sure why. Needs to be fixed
      case e: JobCoinInputException => BadRequest(e)
      case e: Exception => InternalServerError(e)
    }


  def sendCoins(transferService: TransferService): Endpoint[IO, Unit] =
    post("sendCoins" :: jsonBody[SendCoinsRequest]) { request: SendCoinsRequest =>
      transferService
        .sendCoins(request.fromAddress, request.toAddress, request.amount)
        .map(_ => Ok())
    } handle {
      // TODO: This always returns server error no matter what, not sure why. Needs to be fixed
      case e: JobCoinInputException => BadRequest(e)
      case e: Exception => InternalServerError(e)
    }

  def createMixer(mixerService: MixerService): Endpoint[IO, DepositAddressResponse] =
    post("createMixer" :: jsonBody[CreateMixerRequest]) { request: CreateMixerRequest =>
      mixerService
        .createMixerAddresses(request.addresses)
        .map(depositAddress => Ok(DepositAddressResponse(depositAddress.name)))
    } handle {
      // TODO: This always returns server error no matter what, not sure why. Needs to be fixed
      case e: JobCoinInputException => BadRequest(e)
      case e: Exception => InternalServerError(e)
    }
}
