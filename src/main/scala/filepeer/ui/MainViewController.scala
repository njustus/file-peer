package filepeer.ui

import javafx.fxml.FXML
import javafx.scene.control.ListView
import javafx.scene.control.TitledPane
import filepeer.core.discovery.DiscoveryService
import javafx.beans.binding.Bindings
import javafx.collections.FXCollections


class MainViewController {
  import scala.jdk.CollectionConverters._

  @FXML
  var serverListView: ListView[DiscoveryService.ClientName] = null

  @FXML
  var serverInfoView: TitledPane = null

  def connectUiState(state: UiState): Unit = {

    serverListView.itemsProperty.bind(
      Bindings.createObjectBinding(() => FXCollections.observableList(state.availableServers$.getValue.asJava), state.availableServers$)
    )
  }
}
