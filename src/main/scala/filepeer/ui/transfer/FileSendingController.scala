package filepeer.ui.transfer

import java.net.URL
import java.util.ResourceBundle

import cats.data.NonEmptyList
import com.typesafe.scalalogging.LazyLogging
import filepeer.core.Env
import filepeer.core.discovery.DiscoveryService.ClientName
import filepeer.core.transfer.{Client, HttpClient}
import filepeer.ui.components.NotificationDialog
import filepeer.ui.state.{UiState, UiStateController}
import javafx.application.Platform
import javafx.fxml.{FXML, Initializable}
import javafx.scene.control.Label
import javafx.scene.input.{DragEvent, TransferMode}
import javafx.scene.layout.{HBox, StackPane, VBox}
import javafx.stage.Window
import rx.lang.scala.Subscription
import rx.lang.scala.subscriptions.CompositeSubscription

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class FileSendingController(override val env:Env,
                            fileSender: HttpClient)
  extends FileSavedNotificator
    with LazyLogging
    with Initializable
    with UiStateController  {

  import scala.jdk.CollectionConverters._
  import scala.language.implicitConversions


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
          .onComplete {
            case Failure(exception) =>
              logger.error(s"upload for file: ${paths.head} failed.", exception)
            case Success(r:Client.Rejected) =>
              logger.warn("upload rejected.\n"+r.reason)
              Platform.runLater(() => dragDropPane.toFront())
            case Success(Client.Done) =>
              logger.info(s"$files send to $address")
              Platform.runLater {() =>
                dragDropPane.toFront()
                NotificationDialog.notifyAndWait(s"""Uploaded $files""")
              }
            case x => logger.error("ouh no! what happened?", x)
          }
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
