package vinhhv.io.jobcoin
package repository

import java.util.concurrent.ConcurrentHashMap

import cats.effect.IO
import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._
import vinhhv.io.jobcoin.models.{Address, FundType, Funds}

final class JobCoinAPIFake extends CoinRepository {
  val bank = new ConcurrentHashMap[String, Double]()

  final case class FakeAddressInfo(balance: String)

  def sendCoins(fromAddress: Address[_], toAddress: Address[_], deposit: Funds[FundType.Deposit]): IO[Unit] = {
    IO.suspend {
      if (!bank.containsKey(fromAddress.name) || bank.get(fromAddress.name) < deposit.amount) {
        IO.raiseError(InsufficientFundsException(fromAddress.name))
      } else {
        val currentFromAmount = bank.getOrDefault(fromAddress.name, 0.toDouble)
        val currentToAmount = bank.getOrDefault(toAddress.name, 0.toDouble)
        bank.put(fromAddress.name, currentFromAmount - deposit.amount)
        bank.put(toAddress.name, currentToAmount + deposit.amount)
        IO.unit
      }
    }
  }

  def getAddressInfo(address: Address[_]): IO[Json] = {
    IO.suspend {
      val balance = bank.getOrDefault(address.name, 0.toDouble)
      IO.pure(FakeAddressInfo(balance.toString).asJson)
    }
  }
}
