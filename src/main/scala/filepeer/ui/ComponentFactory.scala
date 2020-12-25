package filepeer.ui

import javafx.scene.control._
import javafx.scene.layout._
import javafx.scene.Node
import java.io.InputStream
import javafx.fxml.FXMLLoader
import filepeer.core.discovery.DiscoveryService

object ComponentFactory {
  private def resource(fxmlFileName:String): InputStream = ComponentFactory.getClass.getResourceAsStream(s"/views/$fxmlFileName")

  private def newFxmlControllerComponent[N<:Node, C](fxmlFileName:String): (N, C) = {
    val fxmlLoader = new FXMLLoader()
    val view:N = fxmlLoader.load(resource(fxmlFileName))
    val ctrl:C = fxmlLoader.getController
    (view, ctrl)
  }

  def newRootComponent: (VBox, MainViewController) = newFxmlControllerComponent[VBox, MainViewController]("main-view.fxml")

  def newServerListCell: ListCell[DiscoveryService.ClientName] = new ServerListCell()
}
