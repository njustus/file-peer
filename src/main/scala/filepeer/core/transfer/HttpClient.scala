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

  def sendFile(address:Address, files:NonEmptyList[Path]): Future[Client.UploadResult] = {
    val file = files.head

    for {
      entity <- createEntity(file)
      request = HttpRequest(method = HttpMethods.POST,
                            uri = address.uriString+"file-peer/upload",
                            entity = entity)
      _ = logger.info(s"sending $request")
      response <- http.singleRequest(request)
    } yield {
      response.status match {
        case StatusCodes.Forbidden => Client.Rejected(file, address)
        case _ if response.status.isSuccess() => Client.Done
        case _ => throw Client.Error("Error uploading: $file.\n"+response.status.reason)
      }
    }
  }


  private def createEntity(file: Path): Future[RequestEntity] = {
    val entity = HttpEntity(MediaTypes.`application/octet-stream`, Files.size(file), FileIO.fromPath(file))
    val formData = Multipart.FormData(
          Multipart.FormData.BodyPart(
            "file",
            entity,
            Map("filename" -> file.getFileName.toString)))
    Marshal(formData).to[RequestEntity]
  }
}
