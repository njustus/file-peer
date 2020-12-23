package filepeer.core.transfer


import java.nio.file.{Files, Path}
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{IOResult, Materializer}
import akka.stream.scaladsl._
import akka.util.ByteString
import com.typesafe.scalalogging.LazyLogging
import filepeer.core.{Env, TransferEnv}
import filepeer.core.transfer.TransferServer.{FileTransfer, TransferMsg, TransferPreview}
import io.circe._
import io.circe.generic.JsonCodec
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._

import scala.util.{Failure, Success}
import filepeer.core.transfer.ProtocolHandlers.ProtocolMessage


class FileReceiver(observer:FileReceiver.FileSavedObserver)(implicit mat: Materializer, env: Env) extends LazyLogging {
  private val transferEnv = env.transfer

  import mat.executionContext

  def fileWriter: Flow[FileTransfer, IOResult, NotUsed] = {
    Flow[FileTransfer].map { case FileTransfer(fileName, content) =>
      val path = this.targetPath(fileName)
      logger.info(s"saving $fileName at $path")
      val bs = ByteString(content)
      (FileReceiver.FileSaved(fileName, path), bs)
    }
      .mapAsyncUnordered(2) { case (fileSaved, bs) =>
        //TODO configure separte blocking executor
        Source.single(bs).runWith(FileIO.toPath(fileSaved.path)).map { x =>
          observer.fileSaved(fileSaved)
          x
        }
      }
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

}

object FileReceiver {
  case class FileSaved(name:String, path:Path)

  trait FileSavedObserver {
    def fileSaved(file:FileSaved): Unit
  }
}
