package filepeer.core.transfer

import java.net.InetAddress
import java.nio.file.{Files, Path}

import akka.actor.ActorSystem
import akka.stream.{IOResult, Materializer}
import akka.stream.scaladsl.{FileIO, Flow, Sink, Source, Tcp}
import akka.util.ByteString
import better.files.File
import com.typesafe.scalalogging.LazyLogging
import filepeer.core.{Address, Env}

import scala.concurrent.Future

class FileSender()(implicit actorSystem: ActorSystem, mat: Materializer, env: Env) extends LazyLogging {

  def sendFile(address:Address, files:Seq[Path]) = {
//    val sources = files.map(FileSender.sourceFromPath)
//      .foldLeft(Source.empty[ByteString]) { case (src, current) => src.concat(current) }

    val src = FileSender.sourceFromPath(files.head).log("outgoing-file")
    logger.debug(s"sending files: {} to $address", files.map(_.getFileName))
    Tcp().outgoingConnection(address.host, address.port)
      .runWith(src, Sink.foreach(println))
      ._1
  }

  def sendMsg(address:Address, msg:String) = {
    Tcp().outgoingConnection(address.host, address.port)
      .runWith(Source.single(msg).via(ProtocolHandlers.writeTextMessage), Sink.foreach(println))
      ._1
  }
}

object FileSender {
  case class FileContainer(path:Path) extends AnyVal {
    def fileName: String = path.getFileName.toString
    def bytes: Source[ByteString, Future[IOResult]] = FileIO.fromPath(path)
    def size:Long = Files.size(path)
  }

  def source(fileContainer:FileContainer) = {
    val fileHeader = TransferService.FILENAME_HEADER -> fileContainer.fileName
    ProtocolHandlers.binaryMessageFromSource(fileContainer.bytes, fileContainer.size,Seq(fileHeader))
  }

  val sourceFromPath = (source _).compose(FileContainer.apply)
}
