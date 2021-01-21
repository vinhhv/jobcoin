package vinhhv.io.jobcoin

import cats.effect.{ContextShift, IO}
import com.twitter.finagle.http.Status
import io.finch.Input
import io.circe.generic.auto._
import io.finch._
import io.finch.circe._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import vinhhv.io.jobcoin.API.SendCoinsRequest
import vinhhv.io.jobcoin.models.{Address, AddressType, DepositLog, DistributionAddresses, FundType, Funds}
import vinhhv.io.jobcoin.repository.{HouseTransferQueue, JobCoinAPIFake, MixerRepository}
import vinhhv.io.jobcoin.service.TransferService

final class EndpointsSpec extends AnyFlatSpec with should.Matchers with ScalaCheckPropertyChecks {
  val jobCoinAPIFake = new JobCoinAPIFake()
  val mixerRepo = new MixerRepository() {
    def createMixerPipeline(
        depositAddress: Address[AddressType.Deposit],
        houseAddress: Address[AddressType.House],
        sinkAddresses: List[Address[AddressType.Standard]]
    ): IO[Unit] = IO.unit
    def getDistributionAddresses: IO[List[DistributionAddresses]] = IO.pure(List.empty[DistributionAddresses])

    def isDepositAddress(name: String): IO[Boolean] = IO.pure(false)

    def getHouseAddress(depositAddress: Address[AddressType.Deposit]): IO[Address[AddressType.House]] =
      IO.fromTry(Address.create[AddressType.House](s"house-of-${depositAddress.name}"))
  }
  val queue = new HouseTransferQueue {
    def get(implicit cs: ContextShift[IO]): IO[Option[DepositLog]] = IO.pure(None)

    def add(depositAddress: Address[AddressType.Deposit], amount: Funds[FundType.Deposit]): IO[Unit] = IO.unit
  }
  val mockTransferService = new TransferService(jobCoinAPIFake, mixerRepo, queue)


  jobCoinAPIFake.bank.put("a", 100000000)
  jobCoinAPIFake.bank.put("b", 0)
  "Missing parameters" should "cause an error when passed to an endpoint" in {

    val missingArguments0 = Input.post("/sendCoins").withBody[Application.Json](
      Map("toAddress" -> "b", "amount" -> "10.0")
    )
    val missingArguments1 = Input.post("/sendCoins").withBody[Application.Json](
      Map("fromAddress" -> "a", "amount" -> "10.0")
    )
    val missingArguments2 = Input.post("/sendCoins").withBody[Application.Json](
      Map("fromAddress" -> "a", "toAddress" -> "b")
    )

    List(missingArguments0, missingArguments1, missingArguments2) foreach { input =>
      Endpoints
        .sendCoins(mockTransferService)(input)
        .awaitOutputUnsafe()
        .map(_.status) shouldEqual Some(Status.InternalServerError)
    }
  }

  "Valid parameters" should "return an Ok status" in {
    val validInput = Input.post("/sendCoins").withBody[Application.Json](
      Map("fromAddress" -> "a", "toAddress" -> "b", "amount" -> "1000")
    )

    Endpoints
      .sendCoins(mockTransferService)(validInput)
      .awaitOutputUnsafe()
      .map(_.status) shouldEqual Some(Status.Ok)

  }
}
