package filepeer.ui

import java.awt.Desktop
import java.net.URL
import java.nio.file.Files
import java.util.ResourceBundle

import cats.data.NonEmptyList
import com.typesafe.scalalogging.LazyLogging
import filepeer.core.Env
import filepeer.core.discovery.DiscoveryService.ClientName
import filepeer.core.transfer.Client
import filepeer.ui.state.{UiState, UiStateController}
import javafx.application.Platform
import javafx.fxml.{FXML, Initializable}
import javafx.scene.control.Label
import javafx.scene.input.{DragEvent, TransferMode}
import javafx.scene.layout.HBox
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.stage.Window
import rx.lang.scala.Subscription
import rx.lang.scala.subscriptions.CompositeSubscription

import scala.concurrent.ExecutionContext

class FileSendingController(override val env:Env,
                            fileSender: Client)
  extends FileSavedNotificator
    with LazyLogging
    with Initializable
    with UiStateController  {

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

  override protected def currentWindow: Window = stackPane.getScene.getWindow
  override protected implicit def blockingExecutor: ExecutionContext = ExecutionContext.global
}
