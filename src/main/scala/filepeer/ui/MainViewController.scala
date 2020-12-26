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
import javafx.scene.layout.StackPane
import javafx.scene.control.SelectionMode


class MainViewController extends LazyLogging with CallbackImplicits with Initializable {

  import scala.language.implicitConversions
  import scala.jdk.CollectionConverters._

  @FXML var serverListView: ListView[DiscoveryService.ClientName] = null

  @FXML var dragDropPane: StackPane = null

  @FXML var clientDetailsController: ClientDetailsController = null

  override def initialize(location: URL, resource: ResourceBundle): Unit = {
    // serverListView.setCellFactory(_ => ComponentFactory.newServerListCell)

    require(clientDetailsController != null, "injected child controller 'ClientDetailsController' can not be null!")
    serverListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE)
    serverListView.getSelectionModel().selectedItemProperty().addListener(clientDetailsController.update _)
  }


  def connectUiState(state: UiState): Unit = {
    serverListView.itemsProperty.bind(
      Bindings.createObjectBinding(() => FXCollections.observableList(state.availableServers$.getValue.asJava), state.availableServers$)
    )
  }
}