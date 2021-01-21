package vinhhv.io.jobcoin.repository
import cats.effect.IO
import io.circe.Json
import vinhhv.io.jobcoin.models.{Address, FundType, Funds}

final class JobCoinAPIFake extends CoinRepository {
  def sendCoins(fromAddress: Address[_], toAddress: Address[_], deposit: Funds[FundType.Deposit]): IO[Unit] = ???

  def getAddressInfo(address: Address[_]): IO[Json] = ???
}
