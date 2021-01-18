package vinhhv.io.jobcoin
package models

import cats.effect.IO

sealed trait Address {
  val name: String
}
object Address {
  final case class StandardAddress private (name: String) extends Address
  final case class DepositAddress private (name: String) extends Address
  final case class HouseAddress private (name: String) extends Address

  private val ValidAddressRegex = raw"^([a-zA-Z0-9-_]+)$$".r
  def verifyAddressName(name: String): IO[String] =
    name match {
      case ValidAddressRegex(validName: String) => IO.pure(validName)
      case _ => IO.raiseError(InvalidAddressException(name))
    }

  private def create[A <: Address](name: String, f: String => A): IO[A] = {
    for {
      validName <- verifyAddressName(name)
      address = f(validName)
    } yield address
  }

  def createStandard(name: String): IO[StandardAddress] = create(name, StandardAddress)

  def createDeposit(name: String): IO[DepositAddress] = create(name, DepositAddress)

  def createHouse(name: String): IO[HouseAddress] = create(name, HouseAddress)
}
