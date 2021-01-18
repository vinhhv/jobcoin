package vinhhv.io.jobcoin.requests

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._

final case class CreateMixerRequest(addresses: List[String])
object CreateMixerRequest {
  implicit val decoder: Decoder[CreateMixerRequest] = deriveDecoder[CreateMixerRequest]
  implicit val encoder: Encoder[CreateMixerRequest] = deriveEncoder[CreateMixerRequest]
}
