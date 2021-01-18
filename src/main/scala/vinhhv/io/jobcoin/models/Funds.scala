package vinhhv.io.jobcoin
package models

import cats.effect.IO

sealed trait Funds {
  val amount: Double
}
object Funds {
  final case class Balance private(amount: Double) extends Funds
  final case class Deposit private(amount: Double) extends Funds

  def createBalance(amount: Double): IO[Balance] = {
    if (amount < 0.0) IO.raiseError(IncorrectBalanceException(amount))
    else IO.pure(Balance(amount))
  }

  def createDeposit(amount: Double): IO[Deposit] = {
    if (amount < Double.MinPositiveValue) IO.raiseError(IncorrectDepositException(amount))
    else IO.pure(Deposit(amount))
  }
}


