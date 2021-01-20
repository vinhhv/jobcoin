package vinhhv.io.jobcoin
package models

import scala.util.{Failure, Success, Try}

sealed trait AddressType
object AddressType {
  sealed trait Standard extends AddressType
  sealed trait Deposit extends AddressType
  sealed trait House extends AddressType
}

final case class Address[A <: AddressType] private (name: String)
object Address {
  private val ValidAddressRegex = raw"^([a-zA-Z0-9-_]+)$$".r
  def verifyAddressName(name: String): Try[String] =
    name match {
      case ValidAddressRegex(validName: String) => Success(validName)
      case _ => Failure(InvalidAddressException(name))
    }

  def create[A <: AddressType](name: String): Try[Address[A]] = {
    for {
      validName <- verifyAddressName(name)
    } yield Address[A](validName)
  }
}
