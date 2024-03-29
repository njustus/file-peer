package filepeer.ui

import java.net.URL
import java.util.ResourceBundle

import com.typesafe.scalalogging.LazyLogging
import filepeer.core.discovery.DiscoveryService
import filepeer.ui.state.{UiState, UiStateController}
import javafx.fxml.{FXML, Initializable}
import javafx.scene.control.{Label, TitledPane}
import rx.lang.scala.Subscription

class ClientDetailsController() extends LazyLogging with Initializable with UiStateController {

  @FXML var serverInfoView:  TitledPane = null

  @FXML var serverNameLbl: Label = null

  @FXML var ipLbl:  Label = null

  @FXML var lastConnectedLbl:  Label = null

  @FXML var sharedFilesLbl:  Label = null

  override def initialize(location: URL, resources: ResourceBundle): Unit = {
    lastConnectedLbl.setText(0.toString)
    sharedFilesLbl.setText(0.toString)
  }

  override def connectUiState(state: UiState): Subscription = state.currentClient$.subscribe(update _)

  private def update(clientName: DiscoveryService.ClientName): Unit = {
    val localhost = if(clientName.isLocalhost) " (it's you)" else ""

    serverNameLbl.setText(clientName.hostName+localhost)
    ipLbl.setText(clientName.ip+":"+clientName.port)
  }
}
