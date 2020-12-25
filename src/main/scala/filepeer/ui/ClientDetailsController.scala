package filepeer.ui

import javafx.fxml.FXML
import javafx.scene.control.Label
import javafx.scene.control.TitledPane
import javafx.fxml.Initializable
import java.net.URL
import java.util.ResourceBundle

class ClientDetailsController() extends Initializable {

  @FXML var serverInfoView:  TitledPane = null

  @FXML var serverNameLbl: Label = null

  @FXML var ipLbl:  Label = null

  @FXML var lastConnectedLbl:  Label = null

  @FXML var sharedFilesLbl:  Label = null

  override def initialize(location: URL, resources: ResourceBundle): Unit = {

  }

}
