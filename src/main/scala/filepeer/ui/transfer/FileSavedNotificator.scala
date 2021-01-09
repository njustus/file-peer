package filepeer.ui.transfer

import java.awt.Desktop
import java.nio.file.Files

import com.typesafe.scalalogging.LazyLogging
import filepeer.core.Env
import filepeer.core.transfer.FileReceiver
import javafx.application.Platform
import javafx.event.ActionEvent
import javafx.stage.Window
import javafx.util.Duration
import org.controlsfx.control.Notifications
import org.controlsfx.control.action.Action

import scala.concurrent.{ExecutionContext, Future, blocking}

private[transfer] trait FileSavedNotificator {
  self: LazyLogging =>

  protected def notifyFileSaved(fileSaved: FileReceiver.FileSaved): Unit = {
    logger.info(s"File received: $fileSaved")
    val openDownloadDirectoryAction = new Action("Open Directory", (_: ActionEvent) => this.openDownloadDirectory())
    val openFileAction = new Action("Open File", (_: ActionEvent) => this.openFile(fileSaved))

    val size = Files.size(fileSaved.path).toDouble / 1024 //TODO use message header instead of recalculation

    Platform.runLater { () =>
      Notifications.create()
        .owner(currentWindow)
        .title("File received.")
        .hideAfter(Duration.seconds(5))
        .action(openFileAction, openDownloadDirectoryAction)
        .text(f"Received: ${fileSaved.name} ($size%.2f KB)")
        .showInformation()
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

  protected def currentWindow: Window

  protected implicit def blockingExecutor: ExecutionContext
}
