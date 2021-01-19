package vinhhv.io.jobcoin

import cats.effect.{IO, Timer}
import cats.syntax.apply._
import com.twitter.finagle.{Http, Service}
import com.twitter.finagle.http.{Request, Response}
import com.twitter.util.{Await, FuturePool}
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

  def getAddressBalance: Endpoint[IO, BalanceResponse] = get("balance" :: path[String]) { name: String =>
    FuturePool.unboundedPool {
      transferService
        .getAddressInfo(name)
        .map(addressInfo => Ok(BalanceResponse(addressInfo.balance.amount.toString)))
        .unsafeRunSync()
    } handle {
      // TODO: This always returns server error no matter what, not sure why. Needs to be fixed
      case e: JobCoinInputException => BadRequest(e)
      case e: Exception => InternalServerError(e)
    }
  }

  val sendCoinsRequest: Endpoint[IO, SendCoinsRequest] =
    (param("toAddress") :: param("fromAddress") :: param[Double]("amount")).as[SendCoinsRequest]

  def sendCoins: Endpoint[IO, Unit] =
    post("sendCoins" :: sendCoinsRequest) { request: SendCoinsRequest =>
      FuturePool.unboundedPool {
        transferService
          .sendCoins(request.fromAddress, request.toAddress, request.amount)
          .map(_ => Ok())
          .unsafeRunSync
      }
    } handle {
      // TODO: This always returns server error no matter what, not sure why. Needs to be fixed
      case e: JobCoinInputException => BadRequest(e)
      case e: Exception => InternalServerError(e)
    }

  def createMixer: Endpoint[IO, DepositAddressResponse] =
    post("createMixer" :: jsonBody[CreateMixerRequest]) { request: CreateMixerRequest =>
      FuturePool.unboundedPool {
        mixerService
          .createMixerAddresses(request.addresses)
          .map(depositAddress => Ok(DepositAddressResponse(depositAddress.name)))
          .unsafeRunSync
      }
    } handle {
      // TODO: This always returns server error no matter what, not sure why. Needs to be fixed
      case e: JobCoinInputException => BadRequest(e)
      case e: Exception => InternalServerError(e)
    }

  def service: Service[Request, Response] = Bootstrap
    .serve[Text.Plain](healthcheck)
    .serve[Application.Json](getAddressBalance :+: sendCoins :+: createMixer)
    .toService

  def transferDepositsScheduler: IO[Unit] =
    IO.suspend(houseTransferService.startTransfers) *>
      IO.sleep(20 seconds) *>
      IO.suspend(transferDepositsScheduler)

  def distributeHousesScheduler: IO[Unit] =
    IO.suspend(mixerService.distributeJobcoin) *>
      IO.sleep(10 seconds) *>
      IO.suspend(transferDepositsScheduler)

  val app = for {
    _ <- transferDepositsScheduler.start
    _ <- distributeHousesScheduler.start
    _ <- IO(Await.ready(Http.server.serve(":8081", service)))
  } yield ()
  app.unsafeRunSync()
}