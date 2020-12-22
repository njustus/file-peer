package filepeer.core.transfer

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl._
import akka.util.ByteString
import com.typesafe.scalalogging.LazyLogging
import filepeer.core.{Env, TransferEnv}
import filepeer.core.transfer.TransferService.{FileTransfer, TransferMsg, TransferPreview}
import io.circe._
import io.circe.generic.JsonCodec
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._

trait JsonFormats extends LazyLogging {
  val readJson: Flow[ByteString, TransferMsg, NotUsed] = JsonFraming.objectScanner(Int.MaxValue)
      .map { bs => decode[TransferPreview](bs.utf8String).toTry }
      .flatMapConcat {
        case scala.util.Success(obj) => Source.single(obj)
        case scala.util.Failure(ex) =>
          logger.error("couldn't deserialize msg while reading from connection!", ex)
          Source.empty
      }

  def writeJson[T: Encoder]: Flow[T, ByteString, NotUsed] = Flow[T].map { x => ByteString(x.asJson.noSpaces) }

}
