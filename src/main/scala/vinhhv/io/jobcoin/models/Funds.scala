package vinhhv.io.jobcoin
package models

import scala.util.{Failure, Success, Try}

sealed trait FundType
object FundType {
  sealed trait Balance extends FundType
  sealed trait Deposit extends FundType
}

final case class Funds[F <: FundType](amount: Double)
object Funds {
  def createBalance(amount: Double): Try[Funds[FundType.Balance]] = {
    if (amount < 0.0) Failure(IncorrectBalanceException(amount))
    else Success(Funds[FundType.Balance](amount))
  }

  def createDeposit(amount: Double): Try[Funds[FundType.Deposit]] = {
    if (amount < Double.MinPositiveValue) Failure(IncorrectDepositException(amount))
    else Success(Funds[FundType.Deposit](amount))
  }
}


