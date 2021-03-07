package filepeer.core.transfer

import java.nio.file.{Files, Path}

import akka.stream.IOResult
import akka.stream.scaladsl.{FileIO, Source}
import akka.util.ByteString
import filepeer.core.Address

import scala.concurrent.Future

object Client {
  case class FileContainer(path:Path) extends AnyVal {
    def fileName: String = path.getFileName.toString
    def bytes: Source[ByteString, Future[IOResult]] = FileIO.fromPath(path)
    def size:Long = Files.size(path)
  }

  sealed trait UploadResult
  case object Done extends UploadResult
  case class Rejected(sourceFile:Path, server: Address) extends UploadResult {
    def reason: String = s"${sourceFile.getFileName} was rejected by ${server.format}."
  }
  case class Error(reason: String) extends RuntimeException(reason) with UploadResult
}
