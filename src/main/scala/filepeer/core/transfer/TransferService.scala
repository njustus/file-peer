package filepeer.core.transfer

import java.nio.file.Path

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl._
import akka.util.ByteString
import com.typesafe.scalalogging.LazyLogging
import filepeer.core.Env
import filepeer.core.transfer.TransferService.{TransferMsg, TransferPreview}
import io.circe._
import io.circe.generic.JsonCodec
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._

import scala.util.{Failure, Success}

class TransferService()(implicit actorSystem: ActorSystem, mat: Materializer, env: Env) extends LazyLogging {
  Tcp().bind(env.transfer.address.host, env.transfer.address.port).runForeach { connection =>
    val flow = readJson
      .via(handlingFlow)
      .via(writeJson)

    connection.handleWith(flow)
  }

  val readJson: Flow[ByteString, TransferMsg, NotUsed] = JsonFraming.objectScanner(Int.MaxValue)
      .map { bs => decode[TransferPreview](bs.utf8String).toTry }
      .flatMapConcat {
        case Success(obj) => Source.single(obj)
        case Failure(ex) =>
          logger.error("couldn't deserialize msg while reading from connection!", ex)
          Source.empty
      }

  def writeJson[T: Encoder]: Flow[T, ByteString, NotUsed] = Flow[T].map { x => ByteString(x.asJson.noSpaces) }

  def handlingFlow = {
    Flow[TransferMsg].map { x =>
      logger.debug(s"received: $x")
      x
    }
  }
}

object TransferService {
  @JsonCodec
  sealed trait TransferMsg
  case class TransferPreview(fileNames: List[String]) extends TransferMsg
}
