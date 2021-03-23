package filepeer.ui

import java.net.URL
import java.util.ResourceBundle
import com.typesafe.scalalogging.LazyLogging
import filepeer.core.discovery.DiscoveryService
import filepeer.ui.components.ComponentFactory
import filepeer.ui.state.actions.UpdateCurrentClient
import filepeer.ui.state.{UiState, UiStateController}
import filepeer.ui.transfer.FileSendingController
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.fxml.{FXML, Initializable}
import javafx.scene.control.{ListView, SelectionMode}
import rx.lang.scala.Subscription
import rx.lang.scala.subscriptions.CompositeSubscription

class MainViewController(componentFactory: ComponentFactory) extends LazyLogging with Initializable with CallbackImplicits with UiStateController {

  import scala.jdk.CollectionConverters._
  import scala.language.implicitConversions

  @FXML var serverListView: ListView[DiscoveryService.ClientName] = null

  @FXML var clientDetailsController: ClientDetailsController = null

  @FXML var fileSendingController: FileSendingController = null

  override def initialize(location: URL, resource: ResourceBundle): Unit = {
     serverListView.setCellFactory(_ => componentFactory.newServerListCell)

    require(clientDetailsController != null, "injected child controller 'ClientDetailsController' can not be null!")
    require(fileSendingController != null, "injected child controller 'FileSendingController' can not be null!")
    serverListView.getSelectionModel.setSelectionMode(SelectionMode.SINGLE)
  }

  override def connectUiState(state: UiState): Subscription = {
    val detailsSubscriptions = clientDetailsController.connectUiState(state)
    val sendingSubscriptions = fileSendingController.connectUiState(state)

    val serverSub = state.availableServers$
      .map(xs => FXCollections.observableList(xs.asJava))
      .subscribe { servers =>
        Platform.runLater(() => serverListView.setItems(servers))
      }

    val selectedListener = state.dispatchAction.compose(UpdateCurrentClient.apply)
    serverListView.getSelectionModel.selectedItemProperty.addListener(selectedListener)
    CompositeSubscription(detailsSubscriptions, sendingSubscriptions, serverSub)
  }
}
