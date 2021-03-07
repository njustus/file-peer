package filepeer.core.transfer


import java.nio.file.{Files, Path}
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import akka.NotUsed
import akka.actor.ActorSystem
import akka.dispatch.{Dispatchers, MessageDispatcher}
import akka.http.scaladsl.server.directives.FileInfo
import akka.stream.{IOResult, Materializer}
import akka.stream.scaladsl._
import akka.util.ByteString
import com.typesafe.scalalogging.LazyLogging
import filepeer.core.transfer.FileReceiver.{FileSaved, FileSavedObserver}
import filepeer.core.{Env, TransferEnv}

import scala.concurrent.Future
import scala.language.implicitConversions

class FileReceiver(observer:FileReceiver.FileSavedObserver)(implicit mat: Materializer, env: Env)
  extends LazyLogging {

  private implicit val exec: MessageDispatcher = mat.system.dispatchers.lookup(Dispatchers.DefaultBlockingDispatcherId)

  private def fileWriter(fi: FileInfo): Sink[ByteString, Future[IOResult]] = {
    val path = targetPath(fi.fileName)
    logger.debug(s"saving ${fi.getFieldName} at $path")
    FileIO.toPath(path)
  }

  def targetPath(fileName: String): Path = {
    val timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    val targetFile = env.downloadDir.resolve(fileName)
    if(!Files.exists(targetFile)) {
      targetFile
    } else {
      env.downloadDir.resolve(timestamp+"-"+fileName)
    }
  }

  def accept(fi: FileInfo): Future[Boolean] = {
      val fileName = fi.getFileName
    observer.accept(FileSaved(fileName, targetPath(fileName), None))
  }

  def fileHandler(fi: FileInfo, content: Source[ByteString, NotUsed]): Future[IOResult] = {
    content.runWith[Future[IOResult]](fileWriter(fi)).map { x =>
      val fileName = fi.getFileName
      notify(FileSaved(fileName, targetPath(fileName), Some(x.count)))
      x
    }
  }

  private def notify(fi:FileSaved): Unit = {
    observer.fileSaved(fi)
  }
}

object FileReceiver {
  case class FileSaved(name:String, path:Path, size: Option[Long] = None)

  trait FileSavedObserver {
    def accept(file:FileSaved): Future[Boolean] = Future.successful(true)
    def fileSaved(file:FileSaved): Unit
  }

  def createTargetDir(conf: TransferEnv): Unit = {
    better.files.File(conf.targetDir).createDirectories()
  }
}
