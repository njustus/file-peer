package filepeer.core.transfer

import java.nio.file.{Files, Path}

import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.stream.scaladsl.{FileIO, Source}
import akka.stream.{IOResult, Materializer}
import cats.data.NonEmptyList
import com.typesafe.scalalogging.LazyLogging
import filepeer.core.{Address, Env}

import scala.concurrent.Future

class HttpClient()(implicit mat: Materializer, env: Env) extends LazyLogging {
  import mat.executionContext

  private val http = Http(mat.system)

  def sendFile(address:Address, files:NonEmptyList[Path]): Future[IOResult] = {
    val file = files.head
    val entity = Http

    for {
      entity <- createEntity(file)
      request = HttpRequest(method = HttpMethods.POST,
                            uri = address.uriString+"file-peer/upload",
                            entity = entity)
      _ = logger.info(s"sending $request")
      response <- http.singleRequest(request)
    } yield {
      if(response.status.isSuccess()) IOResult(0)
      else throw new RuntimeException(s"upload for $file failed with: ${response.status}")
    }
  }


  private def createEntity(file: Path): Future[RequestEntity] = {
    val formData = Multipart.FormData(
          Multipart.FormData.BodyPart(
            "file",
            HttpEntity(MediaTypes.`application/octet-stream`, Files.size(file), FileIO.fromPath(file)), // the chunk size here is currently critical for performance
            Map("filename" -> file.getFileName.toString)))
    Marshal(formData).to[RequestEntity]
  }
}
