package filepeer.ui.components

import java.net.URL

import filepeer.core.discovery.DiscoveryService
import filepeer.ui.{DependencyResolver, MainViewController}
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.control.ListCell
import javafx.scene.layout.VBox

class ComponentFactory(diResolver: DependencyResolver) {

  private def newFxmlControllerComponent[N <: Node, C](fxmlFileName: String): (N, C) = {
    val fxmlLoader = new FXMLLoader(ComponentFactory.resource(fxmlFileName))
    fxmlLoader.setControllerFactory(clazz => diResolver.getController(clazz))
    val view: N = fxmlLoader.load()
    val ctrl: C = fxmlLoader.getController
    (view, ctrl)
  }

  def newRootComponent: (VBox, MainViewController) = newFxmlControllerComponent[VBox, MainViewController]("main-view.fxml")

  def newServerListCell: ListCell[DiscoveryService.ClientName] = new ServerListCell()
}

object ComponentFactory {
  private def resource(fxmlFileName: String): URL = ComponentFactory.getClass.getResource(s"/views/$fxmlFileName")
}
