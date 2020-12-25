package filepeer.ui

import javafx.scene.control._
import javafx.scene.layout._
import javafx.scene.Node
import java.io.InputStream
import javafx.fxml.FXMLLoader
import filepeer.core.discovery.DiscoveryService
import java.net.URL

object ComponentFactory {
  private def resource(fxmlFileName:String): URL = ComponentFactory.getClass.getResource(s"/views/$fxmlFileName")

  private def newFxmlControllerComponent[N<:Node, C](fxmlFileName:String): (N, C) = {
    val fxmlLoader = new FXMLLoader(resource(fxmlFileName))
    val view:N = fxmlLoader.load()
    val ctrl:C = fxmlLoader.getController
    (view, ctrl)
  }

  def newRootComponent: (VBox, MainViewController) = newFxmlControllerComponent[VBox, MainViewController]("main-view.fxml")

  def newServerListCell: ListCell[DiscoveryService.ClientName] = new ServerListCell()
}
