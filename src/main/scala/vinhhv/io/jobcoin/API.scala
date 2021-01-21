package vinhhv.io.jobcoin

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._

object API {
  final case class CreateMixerRequest(addresses: List[String])
  implicit val decoder: Decoder[CreateMixerRequest] = deriveDecoder[CreateMixerRequest]
  implicit val encoder: Encoder[CreateMixerRequest] = deriveEncoder[CreateMixerRequest]

  final case class SendCoinsRequest(fromAddress: String, toAddress: String, amount: Double)
  implicit val decoderS: Decoder[SendCoinsRequest] = deriveDecoder[SendCoinsRequest]
  implicit val encoderS: Encoder[SendCoinsRequest] = deriveEncoder[SendCoinsRequest]

  final case class BalanceResponse(balance: String)
  final case class DepositAddressResponse(depositAddress: String)
}
