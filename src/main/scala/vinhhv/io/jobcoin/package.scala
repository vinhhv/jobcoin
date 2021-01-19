package vinhhv.io

package object jobcoin {
  final case class JobCoinAPIException(code: Int, errorMessage: String) extends Exception {
    val message = s"$code: $errorMessage"
  }

  sealed trait JobCoinInputException extends Exception
  final case class InsufficientFundsException(name: String) extends JobCoinInputException {
    val message = s"Insufficient funds for $name"
  }

  final case class IncorrectBalanceException(amount: Double) extends JobCoinInputException {
    val message = s"Balances can only be 0 or a positive double value. Amount specified: $amount"
  }

  final case class IncorrectDepositException(amount: Double) extends JobCoinInputException {
    val message = s"Deposits must be positive double value. Amount specified: $amount"
  }

  final case class InvalidAddressException(name: String) extends JobCoinInputException {
    val message =
      s"Invalid address name provided, must be an alphanumeric, non-empty string. " +
        s"Input provided: $name"
  }

  // Mixer Errors
  final case class DepositAddressAlreadyInUseException(name: String) extends Exception {
    val message = s"Deposit address $name is already in use"
  }

  final case class HouseAddressAlreadyInUseException(name: String) extends Exception {
    val message = s"House address $name is already in use"
  }

  // Generic
  final case class JobCoinServerError(message: String) extends Exception
}
