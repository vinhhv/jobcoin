package vinhhv.io.jobcoin.repository

import cats.effect.IO
import fs2.{Pipe, Stream}
import fs2.concurrent.Topic
import vinhhv.io.jobcoin.models.Address.DepositAddress

final class DepositPubSub {
//  def startPubSub: Stream[IO, Topic[IO, String]] = {
//    for {
//      topic <- Stream.eval(Topic[IO, String]("initial"))
//      _ <- startSubscribers(topic)
//    } yield topic
//  }
//
//  def publish(topic: Topic[IO, String], value: String): Stream[IO, Unit] = {
//    topic.publish(Stream[IO, String](value))
//  }
//
//  def startSubscribers(topic: Topic[IO, String]): Stream[IO, Unit] = {
//    def processAddress: Pipe[IO, String, Unit] =
//      _.flatMap(address => Stream.eval(IO.delay(println(s"Received address: $address"))))
//
//    val addresses: Stream[IO, String] = topic.subscribe(256)
//
//    Stream(addresses.through(processAddress))
//  }
}
