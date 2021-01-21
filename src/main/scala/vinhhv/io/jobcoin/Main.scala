package vinhhv.io.jobcoin

import cats.Parallel
import cats.effect.{ExitCode, IO, IOApp}
import cats.syntax.apply._
import com.twitter.finagle.{Http, ListeningServer, Service}
import com.twitter.finagle.http.{Request, Response}
import io.circe.generic.auto._
import io.finch._
import io.finch.circe._
import io.finch.catsEffect._
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend
import vinhhv.io.jobcoin.Endpoints._
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
      serve: IO[ListeningServer] = IO(Http.server.serve(s":${Settings.PORT}", service(transferService, mixerService)))
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
