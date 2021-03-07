package filepeer.core.transfer

import java.net.InetAddress
import java.nio.file.{Files, Path}

import akka.Done
import akka.actor.ActorSystem
import akka.stream.{IOResult, Materializer}
import akka.stream.scaladsl.{FileIO, Flow, Keep, Sink, Source, Tcp}
import akka.util.ByteString
import better.files.File
import cats.data.NonEmptyList
import com.typesafe.scalalogging.LazyLogging
import filepeer.core.{Address, Env}

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
