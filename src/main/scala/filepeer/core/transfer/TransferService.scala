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

class TransferService()(implicit actorSystem: ActorSystem, mat: Materializer, env: Env) extends LazyLogging {
  val transferEnv = env.transfer
  Tcp().bind(transferEnv.address.host, transferEnv.address.port).runForeach { connection =>
    val sink = ProtocolHandlers.reader
    .via(messageHandler)
    .to(Sink.ignore)

    connection.handleWith(Flow.fromSinkAndSource(sink, Source.empty[ByteString]))
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

  def fileSource(path: Path) = FileIO.fromPath(path)
    .fold(ByteString.empty) { (acc, bs) => acc ++ bs }
    .map { bs => FileTransfer(path.getFileName.toString, bs.toArray) }

  def serializedFileSource(path: Path) = fileSource(path).via(serializeFileTransfer)

  def serializeFileTransfer = Flow[FileTransfer].flatMapConcat { case FileTransfer(fileName, bytes) =>
    val fileHeader = TransferService.FILENAME_HEADER -> fileName
    Source.single(ByteString(bytes))
      .via(ProtocolHandlers.writeBinaryMessage(Seq(fileHeader)))
  }

  def fileSink = {
    Flow[FileTransfer].map { case FileTransfer(fileName, content) =>
        val path = this.targetPath(fileName)
        logger.info(s"saving $fileName at $path")
        val bs = ByteString(content)
        (path, bs)
    }
      .mapAsyncUnordered(2) { case (path, bs) => Source.single(bs).runWith(FileIO.toPath(path)) }
      .to(Sink.ignore)
  }

  private def targetPath(fileName: String): Path = {
    val timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    val targetFile = transferEnv.targetDir.resolve(fileName)
    if(!Files.exists(targetFile)) {
      targetFile
    } else {
      transferEnv.targetDir.resolve(timestamp+"-"+fileName)
    }
  }

  def messageHandler = Flow[ProtocolMessage].flatMapConcat {
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

  def textMessageHandler(textMsg:ProtocolMessage) = {
    Source.single(textMsg.body).via(readJson).log("deserialized-json")
  }

  def binaryMessageHandler(binaryMsg:ProtocolMessage) = {
    binaryMsg.header.get(TransferService.FILENAME_HEADER) match {
      case Some(fileName) =>
        logger.debug(s"got a FileTransfer for fileName:$fileName")
        Source.single(FileTransfer(fileName, binaryMsg.body.toArray))
          .alsoTo(fileSink)
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
