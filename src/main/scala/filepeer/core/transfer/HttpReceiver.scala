package filepeer.core.transfer

import akka.NotUsed
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.directives.FileInfo
import akka.stream.scaladsl.Source
import akka.stream.{IOResult, Materializer}
import akka.util.ByteString
import com.typesafe.scalalogging.LazyLogging
import filepeer.core.{Address, Env}

import scala.concurrent.Future

class HttpReceiver(fileReceiver: FileReceiver)(implicit mat: Materializer, env: Env)
  extends HttpEndpoints
    with LazyLogging {

  private implicit val sys = mat.system
  private implicit val context = mat.executionContext

  override def accept(fi: FileInfo): Future[Boolean] = {
    fileReceiver.accept(fi)
  }

  override def fileHandler(fi: FileInfo, content: Source[ByteString, NotUsed]): Future[IOResult] = {
    fileReceiver.fileHandler(fi, content)
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
