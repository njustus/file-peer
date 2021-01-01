package filepeer.ui

import javafx.fxml.FXML
import javafx.scene.control.ListView
import javafx.scene.control.TitledPane
import filepeer.core.discovery.DiscoveryService
import javafx.beans.binding.Bindings
import javafx.collections.FXCollections
import com.typesafe.scalalogging.LazyLogging
import javafx.fxml.Initializable
import java.net.URL
import java.util.ResourceBundle

import cats.data.NonEmptyList
import filepeer.core.transfer.Client
import filepeer.ui.components.ComponentFactory
import filepeer.ui.state.{UiState, UiStateController}
import filepeer.ui.state.actions.UpdateCurrentClient
import javafx.scene.layout.StackPane
import javafx.scene.control.SelectionMode
import javafx.scene.input.{DragEvent, TransferMode}
import rx.lang.scala.Subscription
import rx.lang.scala.subscriptions.CompositeSubscription
import scala.concurrent.ExecutionContext.Implicits._

class MainViewController extends LazyLogging with Initializable with CallbackImplicits with UiStateController {

  import scala.language.implicitConversions
  import scala.jdk.CollectionConverters._

  @FXML var serverListView: ListView[DiscoveryService.ClientName] = null

  @FXML var dragDropPane: StackPane = null

  @FXML var clientDetailsController: UiStateController = null

  private var fileSender: Client = null

  override def initialize(location: URL, resource: ResourceBundle): Unit = {
     serverListView.setCellFactory(_ => ComponentFactory.newServerListCell)

    require(clientDetailsController != null, "injected child controller 'ClientDetailsController' can not be null!")
    serverListView.getSelectionModel.setSelectionMode(SelectionMode.SINGLE)
  }

  override def connectUiState(state: UiState): Subscription = {
    val childSubscriptions = clientDetailsController.connectUiState(state)

    val serverSub = state.availableServers$
      .map(xs => FXCollections.observableList(xs.asJava))
      .subscribe(serverListView.setItems _)

    val selectedListener = state.dispatchAction.compose(UpdateCurrentClient.apply)
    serverListView.getSelectionModel.selectedItemProperty.addListener(selectedListener)
    CompositeSubscription(childSubscriptions, serverSub)
  }

  def setFileSender(client:Client):Unit = fileSender = client //TODO find better alternative than setter

  def onDragOver(ev:DragEvent): Unit = {
    if(ev.getDragboard.hasFiles) {
      dragDropPane.getStyleClass.addAll("drag-drop-pane--active")
      ev.acceptTransferModes(TransferMode.COPY)
    }
  }

  def onFileDropped(ev:DragEvent): Unit = {
    val board = ev.getDragboard
    val droppedFiles = board.getFiles.asScala.toList
    logger.info(s"dropped files: $droppedFiles")

    dragDropPane.getStyleClass.removeAll("drag-drop-pane--active")

    NonEmptyList.fromList(droppedFiles) match {
      case Some(files) =>
        val address = serverListView.getSelectionModel.getSelectedItem.address
        val paths = files.map(_.toPath)
        fileSender
          .sendFile(address, paths)
          .foreach(_ => logger.info(s"$files send to $address")) //TODO notify UI
      case None => logger.warn("Ignoring dropped empty file list!")
    }
  }
}
