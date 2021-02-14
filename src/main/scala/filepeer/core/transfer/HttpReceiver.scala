package filepeer.core.transfer

import java.nio.file.Path

import akka.NotUsed
import akka.http.javadsl.server.directives.FileInfo
import akka.http.scaladsl.Http
import akka.stream.{IOResult, Materializer}
import akka.stream.scaladsl.{FileIO, Source}
import akka.util.ByteString
import com.typesafe.scalalogging.LazyLogging
import filepeer.core.transfer.FileReceiver.{FileSaved, FileSavedObserver}
import filepeer.core.{Address, Env}

import scala.concurrent.Future

class HttpReceiver(fileAcceptor: FileSavedObserver)(implicit mat: Materializer, env: Env)
  extends HttpEndpoints
    with LazyLogging {

  private implicit val sys = mat.system
  private implicit val context = mat.executionContext

  private def generateFilePath(fileName: String): Path = env.downloadDir.resolve(fileName)

  override def accept(fi: FileInfo): Future[Boolean] = {
    val fileName = fi.getFileName
    fileAcceptor.accept(FileSaved(fileName, generateFilePath(fileName)))
  } //TODO actual impl

  override def fileHandler(fi: FileInfo, content: Source[ByteString, NotUsed]): Future[IOResult] = {
    val fileName = fi.getFileName
    content.runWith[Future[IOResult]](FileIO.toPath(generateFilePath(fileName)))
  }

  def bind(address: Address): Future[Http.ServerBinding] = {
    Http()
      .newServerAt(address.host, address.port)
      .bind(super.routes)
      .map { binding =>
        val addr = binding.localAddress
        logger.debug(s"Bound http to ${addr.getHostName}:${addr.getPort}")
        binding
      }
  }
}
