package vinhhv.io.jobcoin

import cats.Parallel
import cats.effect.{ExitCode, IO, IOApp, Timer}
import cats.syntax.apply._
import com.twitter.finagle.{Http, ListeningServer, Service}
import com.twitter.finagle.http.{Request, Response}
import io.circe.generic.auto._
import io.finch._
import io.finch.catsEffect._
import io.finch.circe._
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend
import vinhhv.io.jobcoin.API._
import vinhhv.io.jobcoin.errorhandling.EncodingImplicits._
import vinhhv.io.jobcoin.repository.{HouseTransferQueueInMemory, JobCoinAPI, MixerRepositoryInMemory}
import vinhhv.io.jobcoin.service.{HouseTransferService, MixerService, TransferService}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object Main extends IOApp {
  implicit val ec = ExecutionContext.global
  implicit val ctx = IO.contextShift(ec)

  def healthcheck: Endpoint[IO, String] = get(pathEmpty) {
    Ok("OK")
  }

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

  val sendCoinsRequest: Endpoint[IO, SendCoinsRequest] =
    (param("toAddress") :: param("fromAddress") :: param[Double]("amount")).as[SendCoinsRequest]

  def sendCoins(transferService: TransferService): Endpoint[IO, Unit] =
    post("sendCoins" :: sendCoinsRequest) { request: SendCoinsRequest =>
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

  def service(transferService: TransferService, mixerService: MixerService): Service[Request, Response] = Bootstrap
    .serve[Text.Plain](healthcheck)
    .serve[Application.Json](getAddressBalance(transferService) :+: sendCoins(transferService) :+: createMixer(mixerService))
    .toService

  def transferDepositsScheduler(houseTransferService: HouseTransferService): IO[Unit] =
    houseTransferService.startTransfers *>
      IO.sleep(Settings.DEPOSIT_SERVICE_SCHEDULE seconds) *>
      IO.suspend(transferDepositsScheduler(houseTransferService))

  def distributeHousesScheduler(mixerService: MixerService): IO[Unit] =
    mixerService.distributeJobcoin *>
      IO.sleep(Settings.MIXER_SERVICE_SCHEDULE seconds) *>
      IO.suspend(distributeHousesScheduler(mixerService))

  override def run(args: List[String]): IO[ExitCode] = {
    val app = for {
      backend <- AsyncHttpClientCatsBackend[IO]()
      jobCoinAPI = new JobCoinAPI(backend)
      mixerRepo = new MixerRepositoryInMemory()
      houseTransferQueue = new HouseTransferQueueInMemory()
      transferService = new TransferService(jobCoinAPI, mixerRepo, houseTransferQueue)
      mixerService = new MixerService(mixerRepo, transferService)
      houseTransferService = new HouseTransferService(transferService, mixerRepo, houseTransferQueue)
      serve: IO[ListeningServer] = IO(Http.server.serve(":8081", service(transferService, mixerService)))
      exit <-
        Parallel
          .parMap3(
            transferDepositsScheduler(houseTransferService),
            distributeHousesScheduler(mixerService),
            serve
          )((_, _, _) => ExitCode.Success)
    } yield exit
    app
  }
}
