package filepeer.core.transfer

import akka.NotUsed
import akka.http.javadsl.server.directives.FileInfo
import com.typesafe.scalalogging.LazyLogging
import akka.http.scaladsl.model.{HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.{Directives, Route}
import akka.stream.IOResult
import akka.stream.scaladsl.Source
import akka.util.ByteString

import scala.concurrent.Future

trait HttpEndpoints
  extends Directives {
  this: LazyLogging =>

  def accept(fi:FileInfo): Future[Boolean]
  def fileHandler(fi:FileInfo, content: Source[ByteString, NotUsed]): Future[IOResult]

  private def innerRoutes: Route = path("upload") {
    extractExecutionContext { implicit exec =>
      (post & fileUpload("file")) { case (fileInfo, contentStream) =>
        logger.info(s"received file: ${fileInfo.fileName}")

        val future: Future[HttpResponse] = accept(fileInfo).flatMap {
          case true =>
            val fixedMatValueStream = contentStream.mapMaterializedValue(_ => NotUsed)
            fileHandler(fileInfo, fixedMatValueStream).map(_ => HttpResponse(StatusCodes.OK))
          case _ => Future.successful(HttpResponse(StatusCodes.Forbidden, entity = s"Upload for ${fileInfo.fileName} rejected!"))
        }

        complete(future)
      }
    }
  }

  def routes: Route = pathPrefix("file-peer") {
    innerRoutes
  }
}
