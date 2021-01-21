package vinhhv.io.jobcoin.service

import cats.effect.{ContextShift, IO}
import org.scalacheck.Gen
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import vinhhv.io.jobcoin.models.{Address, AddressType, DepositLog, DistributionAddresses, FundType, Funds}
import vinhhv.io.jobcoin.repository.{HouseTransferQueue, JobCoinAPIFake, MixerRepository}

final class TransferServiceSpec extends AnyFlatSpec with should.Matchers with ScalaCheckPropertyChecks {
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
  val transferService = new TransferService(jobCoinAPIFake, mixerRepo, queue)

  jobCoinAPIFake.bank.put("fromA", 100000000)
  jobCoinAPIFake.bank.put("fromB", 100000000)
  jobCoinAPIFake.bank.put("fromC", 100000000)
  jobCoinAPIFake.bank.put("fromD", 100000000)
  jobCoinAPIFake.bank.put("fromE", 100000000)

  jobCoinAPIFake.bank.put("toA", 0)
  jobCoinAPIFake.bank.put("toB", 0)
  jobCoinAPIFake.bank.put("toC", 0)
  jobCoinAPIFake.bank.put("toD", 0)
  jobCoinAPIFake.bank.put("toE", 0)

  val transactions: Gen[(String, String, Int)] = for {
    fromAddresses <- Gen.oneOf(List("fromA", "fromB", "fromC", "fromD", "fromE"))
    toAddresses <- Gen.oneOf(List("toA", "toB", "toC", "toD", "toE"))
    amounts <- Gen.choose(1, 1000)
  } yield (fromAddresses, toAddresses, amounts)

  "All transactions losses/gains" should "be reflected in the sender's and receiver's accounts" in {
    forAll(transactions) {
      case (fromAddress, toAddress, amount) =>
        val beforeFromAmount = jobCoinAPIFake.bank.get(fromAddress)
        val beforeToAmount = jobCoinAPIFake.bank.get(toAddress)

        transferService.sendCoins(fromAddress, toAddress, amount).unsafeRunSync

        val afterFromAmount = jobCoinAPIFake.bank.get(fromAddress)
        val afterToAmount = jobCoinAPIFake.bank.get(toAddress)

        (beforeFromAmount - amount) shouldEqual afterFromAmount
        (beforeToAmount + amount) shouldEqual afterToAmount
    }
  }
}
