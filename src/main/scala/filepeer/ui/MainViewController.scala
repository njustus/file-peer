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

import filepeer.ui.components.ComponentFactory
import filepeer.ui.state.UiState
import filepeer.ui.state.actions.UpdateCurrentClient
import javafx.scene.layout.StackPane
import javafx.scene.control.SelectionMode
import javafx.scene.input.{DragEvent, TransferMode}
import rx.lang.scala.Subscription
import rx.subscriptions.Subscriptions


class MainViewController extends LazyLogging with CallbackImplicits with Initializable {

  import scala.language.implicitConversions
  import scala.jdk.CollectionConverters._

  @FXML var serverListView: ListView[DiscoveryService.ClientName] = null

  @FXML var dragDropPane: StackPane = null

  @FXML var clientDetailsController: ClientDetailsController = null

  override def initialize(location: URL, resource: ResourceBundle): Unit = {
     serverListView.setCellFactory(_ => ComponentFactory.newServerListCell)

    require(clientDetailsController != null, "injected child controller 'ClientDetailsController' can not be null!")
    serverListView.getSelectionModel.setSelectionMode(SelectionMode.SINGLE)
  }


  def connectUiState(state: UiState): Unit = {
    clientDetailsController.connectUiState(state)

    state.availableServers$
      .map(xs => FXCollections.observableList(xs.asJava))
      .subscribe(xs => {
        println("new xs: "+xs)
        serverListView.setItems(xs)
      })

    val selectedListener = state.dispatchAction.compose(UpdateCurrentClient.apply)
    serverListView.getSelectionModel.selectedItemProperty.addListener(selectedListener)
  }

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
  }
}
