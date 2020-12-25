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


class MainViewController extends LazyLogging with Initializable {

  import scala.jdk.CollectionConverters._

  @FXML var serverListView: ListView[DiscoveryService.ClientName] = null

  @FXML var serverInfoView: TitledPane = null

  @FXML var dragDropPane: StackPane = null

  override def initialize(location: URL, resource: ResourceBundle): Unit = {
    serverListView.setCellFactory(_ => ComponentFactory.newServerListCell)
  }


  def connectUiState(state: UiState): Unit = {
    serverListView.itemsProperty.bind(
      Bindings.createObjectBinding(() => FXCollections.observableList(state.availableServers$.getValue.asJava), state.availableServers$)
    )
  }
}
