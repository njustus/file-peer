package filepeer.core.transfer

import java.nio.file.{Files, Path}
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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

import scala.util.{Failure, Success}
import filepeer.core.transfer.ProtocolHandlers.ProtocolMessage

class TransferService(fileReceiver: FileReceiver)(implicit actorSystem: ActorSystem, mat: Materializer, env: Env) extends LazyLogging with JsonFormats {
  private val transferEnv = env.transfer

  Tcp().bind(transferEnv.address.host, transferEnv.address.port).runForeach { connection =>
    val sink = Flow[ByteString].log("incoming-tcp")
      .via(ProtocolHandlers.reader)
    .via(messageHandler)
      .to(Sink.ignore)

    val initialMsgSrc = Source.single[ByteString](ByteString("initial message"))

    logger.debug("new connection from: {}", connection.remoteAddress)
    connection.handleWith(Flow.fromSinkAndSource(sink, initialMsgSrc))
  }

  def messageHandler: Flow[ProtocolMessage, Object, NotUsed] = Flow[ProtocolMessage].flatMapConcat {
    case textMsg:ProtocolMessage if textMsg.isTextMessage =>
      logger.debug(s"received text msg: $textMsg")
      textMessageHandler(textMsg)
    case binaryMsg:ProtocolMessage if binaryMsg.isBinaryMessage =>
      logger.debug(s"received binary msg: $binaryMsg")
      binaryMessageHandler(binaryMsg)
    case msg =>
      logger.warn(s"unknown message type: ${msg.messageType} for msg: $msg. DROPPING IT!")
      Source.empty
  }

  private def textMessageHandler(textMsg:ProtocolMessage) = {
    Source.single(textMsg.body).via(readJson).log("deserialized-json")
  }

  private def binaryMessageHandler(binaryMsg:ProtocolMessage) = {
    binaryMsg.header.get(TransferService.FILENAME_HEADER) match {
      case Some(fileName) =>
        logger.info(s"got a FileTransfer for fileName:$fileName")
        Source.single(FileTransfer(fileName, binaryMsg.body.toArray)).via(fileReceiver.fileWriter)
      case None =>
        logger.warn(s"binary message without a header:${TransferService.FILENAME_HEADER}. DROPPING IT!")
        Source.empty
    }
  }
}

object TransferService {
  @JsonCodec
  sealed trait TransferMsg
  case class TransferPreview(fileNames: List[String]) extends TransferMsg
  case class FileTransfer(fileName: String, content: Array[Byte]) extends TransferMsg

  val FILENAME_HEADER = "File-Name"

  def createTargetDir(conf: TransferEnv): Unit = {
    better.files.File(conf.targetDir).createDirectories()
  }
}
