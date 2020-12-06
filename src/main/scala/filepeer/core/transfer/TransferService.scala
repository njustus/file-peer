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

class TransferService()(implicit actorSystem: ActorSystem, mat: Materializer, env: Env) extends LazyLogging {
  val transferEnv = env.transfer
  Tcp().bind(transferEnv.address.host, transferEnv.address.port).runForeach { connection =>
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

  def fileSource(path: Path) = FileIO.fromPath(path)
    .fold(ByteString.empty) { (acc, bs) => acc ++ bs }
    .map { bs => FileTransfer(path.getFileName.toString, bs.toArray) }

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

  def targetPath(fileName: String): Path = {
    val timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    val targetFile = transferEnv.targetDir.resolve(fileName)
    if(!Files.exists(targetFile)) {
      targetFile
    } else {
      transferEnv.targetDir.resolve(timestamp+"-"+fileName)
    }
  }

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
  case class FileTransfer(fileName: String, content: Array[Byte]) extends TransferMsg

  def createTargetDir(conf: TransferEnv): Unit = {
    better.files.File(conf.targetDir).createDirectories()
  }
}
