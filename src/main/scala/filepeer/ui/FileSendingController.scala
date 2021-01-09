package filepeer.ui

import java.awt.Desktop
import java.net.URL
import java.nio.file.Files
import java.util.ResourceBundle

import cats.data.NonEmptyList
import com.typesafe.scalalogging.LazyLogging
import filepeer.core.Env
import filepeer.core.discovery.DiscoveryService.ClientName
import filepeer.core.transfer.{Client, FileReceiver}
import filepeer.ui.state.{UiState, UiStateController}
import javafx.application.Platform
import javafx.event.ActionEvent
import javafx.fxml.{FXML, Initializable}
import javafx.scene.control.Label
import javafx.scene.input.{DragEvent, TransferMode}
import javafx.scene.layout.HBox
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.util.Duration
import org.controlsfx.control.Notifications
import org.controlsfx.control.action.Action
import rx.lang.scala.Subscription
import rx.lang.scala.subscriptions.CompositeSubscription

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.{Future, blocking}

class FileSendingController(env:Env, fileSender: Client) extends LazyLogging with Initializable with UiStateController  {

  import scala.language.implicitConversions
  import scala.jdk.CollectionConverters._


  @FXML var stackPane: StackPane = null

  @FXML var dragDropPane: HBox = null

  @FXML var progressPane: VBox = null

  @FXML var progressLlbl: Label = null

  private var selectedClient:Option[ClientName] = None

  @FXML
  def onDragOver(ev:DragEvent): Unit = {
    if(ev.getDragboard.hasFiles && selectedClient.isDefined) {
      dragDropPane.getStyleClass.addAll("drag-drop-pane--active")
      ev.acceptTransferModes(TransferMode.COPY)
    } else {
      logger.trace(s"ignoring DragEvent because: hasFiles?${ev.getDragboard.hasFiles} or selectedClient?${selectedClient.isDefined}")
    }
  }

  @FXML
  def onFileDropped(ev:DragEvent): Unit = {
    val board = ev.getDragboard
    val droppedFiles = board.getFiles.asScala.toList
    logger.info(s"dropped files: $droppedFiles")

    dragDropPane.getStyleClass.removeAll("drag-drop-pane--active")

    val opt = for {
      files <- NonEmptyList.fromList(droppedFiles)
      client <- selectedClient
    } yield {
      progressPane.toFront()

      val address = client.address
        val paths = files.map(_.toPath)
        fileSender
          .sendFile(address, paths)
          .foreach { _ =>
            logger.info(s"$files send to $address")
            Platform.runLater(() => dragDropPane.toFront())
          } //TODO notify UI
    }

    opt.getOrElse(logger.warn("Ignoring dropped empty file list!"))
  }

  override def initialize(location: URL, resources: ResourceBundle): Unit = {
    dragDropPane.toFront()
  }

  override def connectUiState(state: UiState): Subscription = {
    val clientSub = state.currentClient$.subscribe(cl => selectedClient = Some(cl))
    val fsSub = state.filSaved$.subscribe(this.notifyFileSaved _)

    CompositeSubscription(clientSub, fsSub)
  }

  private def notifyFileSaved(fileSaved: FileReceiver.FileSaved): Unit = {
    logger.info(s"File received: $fileSaved")
    val openDownloadDirectoryAction = new Action("Open Directory", (_:ActionEvent) => this.openDownloadDirectory())
    val openFileAction = new Action("Open File", (_:ActionEvent) => this.openFile(fileSaved))

    val size = Files.size(fileSaved.path).toDouble / 1024 //TODO use message header instead of recalculation

    Platform.runLater { () =>
      Notifications.create()
        .owner(stackPane.getScene.getWindow)
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
    if(Desktop.isDesktopSupported) {
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
}
