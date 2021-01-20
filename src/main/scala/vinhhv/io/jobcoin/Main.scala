package vinhhv.io.jobcoin

import cats.effect.{IO, Timer}
import cats.syntax.apply._
import com.twitter.finagle.{Http, Service}
import com.twitter.finagle.http.{Request, Response}
import com.twitter.util.Await
import io.circe.generic.auto._
import io.finch._
import io.finch.catsEffect._
import io.finch.circe._
import vinhhv.io.jobcoin.API._
import vinhhv.io.jobcoin.errorhandling.EncodingImplicits._
import vinhhv.io.jobcoin.repository.{HouseTransferQueueInMemory, JobCoinAPI, MixerRepositoryInMemory}
import vinhhv.io.jobcoin.service.{HouseTransferService, MixerService, TransferService}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object Main extends App {
  implicit val ec = ExecutionContext.global
  implicit val ctx = IO.contextShift(ec)
  implicit val timer: Timer[IO] = IO.timer(ec)

  val jobCoinAPI = new JobCoinAPI()
  val mixerRepo = new MixerRepositoryInMemory()
  val houseTransferQueue = new HouseTransferQueueInMemory()

  val transferService = new TransferService(jobCoinAPI, mixerRepo, houseTransferQueue)
  val mixerService = new MixerService(mixerRepo, transferService)
  val houseTransferService = new HouseTransferService(transferService, mixerRepo, houseTransferQueue)

  def healthcheck: Endpoint[IO, String] = get(pathEmpty) {
    Ok("OK")
  }

  def getAddressBalance: Endpoint[IO, BalanceResponse] =
    get("balance" :: path[String]) { name: String =>
      transferService
        .getAddressInfo(name)
        .map(addressInfo => Ok(BalanceResponse(addressInfo.balance.amount.toString)))
    } handle {
      // TODO: This always returns server error no matter what, not sure why. Needs to be fixed
      case e: JobCoinInputException => BadRequest(e)
      case e: Exception => InternalServerError(e)
    }

  val sendCoinsRequest: Endpoint[IO, SendCoinsRequest] =
    (param("toAddress") :: param("fromAddress") :: param[Double]("amount")).as[SendCoinsRequest]

  def sendCoins: Endpoint[IO, Unit] =
    post("sendCoins" :: sendCoinsRequest) { request: SendCoinsRequest =>
      transferService
        .sendCoins(request.fromAddress, request.toAddress, request.amount)
        .map(_ => Ok())
    } handle {
      // TODO: This always returns server error no matter what, not sure why. Needs to be fixed
      case e: JobCoinInputException => BadRequest(e)
      case e: Exception => InternalServerError(e)
    }

  def createMixer: Endpoint[IO, DepositAddressResponse] =
    post("createMixer" :: jsonBody[CreateMixerRequest]) { request: CreateMixerRequest =>
      mixerService
        .createMixerAddresses(request.addresses)
        .map(depositAddress => Ok(DepositAddressResponse(depositAddress.name)))
    } handle {
      // TODO: This always returns server error no matter what, not sure why. Needs to be fixed
      case e: JobCoinInputException => BadRequest(e)
      case e: Exception => InternalServerError(e)
    }

  def service: Service[Request, Response] = Bootstrap
    .serve[Text.Plain](healthcheck)
    .serve[Application.Json](getAddressBalance :+: sendCoins :+: createMixer)
    .toService

  val transferDepositsScheduler: IO[Unit] =
    houseTransferService.startTransfers *>
      IO.sleep(20 seconds) *>
      transferDepositsScheduler

  val distributeHousesScheduler: IO[Unit] =
    mixerService.distributeJobcoin *>
      IO.sleep(10 seconds) *>
      distributeHousesScheduler

  val app = for {
    fiber1 <- transferDepositsScheduler.start
    fiber2 <- distributeHousesScheduler.start
    result <-
      IO(Await.ready(Http.server.serve(":8081", service)))
        .handleErrorWith { error =>
          fiber1.cancel *> fiber2.cancel *> IO.raiseError(error)
        }
  } yield result
  app.unsafeRunSync()
}