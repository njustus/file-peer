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

@deprecated(message = "use 'HttpClient' instead", since = "v2")
class Client()(implicit mat: Materializer, env: Env) extends LazyLogging {

  def sendFile(address:Address, files:NonEmptyList[Path]): Future[IOResult] = {
    val sources = files.map(Client.sourceFromPath)
      .reduceLeft[Source[ByteString, Future[IOResult]]] { case (src, current) => src.concatMat(current)(Keep.right) }

    logger.info(s"sending files: {} to $address", files.map(_.getFileName))

    val src = Client.sourceFromPath(files.head)
    Tcp()(mat.system).outgoingConnection(address.host, address.port)
      .runWith(sources, Sink.ignore)
      ._1
  }

  def sendMsg(address:Address, msg:String): Future[Done] = {
    val outgoingFLow = Tcp()(mat.system).outgoingConnection(address.host, address.port)
    Source.single(msg)
      .via(ProtocolHandlers.writeTextMessage)
      .via(outgoingFLow)
      .toMat(Sink.ignore)(Keep.right)
      .run()
  }
}

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

  def source(fileContainer:FileContainer): Source[ByteString, Future[IOResult]] = {
    val fileHeader = TransferServer.FILENAME_HEADER -> fileContainer.fileName
    ProtocolHandlers.binaryMessageFromSource(fileContainer.bytes, fileContainer.size,Seq(fileHeader))
  }

  val sourceFromPath: Path => Source[ByteString, Future[IOResult]] = (source _).compose(FileContainer.apply)
}
