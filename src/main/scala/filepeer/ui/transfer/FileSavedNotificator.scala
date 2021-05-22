package filepeer.ui.transfer

import java.awt.Desktop

import com.typesafe.scalalogging.LazyLogging
import filepeer.core.Env
import filepeer.core.transfer.FileReceiver
import filepeer.ui.components.Notifications
import javafx.application.Platform
import javafx.event.ActionEvent
import org.controlsfx.control.action.Action

import scala.concurrent.{ExecutionContext, Future, blocking}

private[transfer] trait FileSavedNotificator extends Notifications {
  self: LazyLogging =>

  protected def notifyFileSaved(fileSaved: FileReceiver.FileSaved): Unit = {
    logger.info(s"File received: $fileSaved")
    val openDownloadDirectoryAction = new Action("Open Directory", (_: ActionEvent) => this.openDownloadDirectory())
    val openFileAction = new Action("Open File", (_: ActionEvent) => this.openFile(fileSaved))
    val notifyReceived = notifyA(openDownloadDirectoryAction, openFileAction) _

    val kbStr = fileSaved.size.map { sz =>
        val kb = sz.toDouble / 1024
        f"$kb%.2f"
      }
      .getOrElse("???")
    val sizeString = s"($kbStr KB)"

    Platform.runLater { () =>
      notifyReceived(s"Received: ${fileSaved.name} $sizeString", "File received.")
    }
  }

  private def openDownloadDirectory(): Unit = {
    withDesktop(_.open(env.downloadDir.toFile))
  }

  private def openFile(fs: FileReceiver.FileSaved): Unit = {
    withDesktop(_.open(fs.path.toFile))
  }

  private def withDesktop[A](fn: Desktop => A): Future[A] = {
    if (Desktop.isDesktopSupported) {
      //must run in separate thread to not block the whole application
      Future {
        blocking {
          fn(Desktop.getDesktop)
        }
      }
    } else {
      throw new UnsupportedOperationException("Unsupported Desktop Environment! Can not open default application.")
    }
  }

  protected def env: Env

  protected implicit def blockingExecutor: ExecutionContext
}
