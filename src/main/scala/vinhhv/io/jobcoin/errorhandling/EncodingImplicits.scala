package vinhhv.io.jobcoin.errorhandling

import io.circe.{Encoder, Json}
import io.finch.{Error, Errors}

object EncodingImplicits {
  def errorToJson(e: Error): Json = e match {
    case Error.NotPresent(item) =>
      Json.obj("error" -> Json.fromString(s"${item.description} is missing."))
    case Error.NotParsed(item, targetType, cause) =>
      Json.obj(
        "error" -> Json.fromString(
          s"Cannot parse ${item.description}. Needs to be a valid ${targetType.toString} type"
        ))
    case Error.NotValid(item, rule) =>
      Json.obj("error" -> Json.fromString(s"Invalid input from ${item.description}: $rule"))
  }

  implicit val ee: Encoder[Exception] = Encoder.instance {
    case e: Error => errorToJson(e)
    case Errors(nel) => Json.arr(nel.toList.map(errorToJson): _*)
  }

}
