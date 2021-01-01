package filepeer.ui.components

import java.net.URL

import filepeer.core.discovery.DiscoveryService
import filepeer.ui.MainViewController
import filepeer.ui.state.UiStateController
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.control.ListCell
import javafx.scene.layout.VBox

object ComponentFactory {
  private def resource(fxmlFileName: String): URL = ComponentFactory.getClass.getResource(s"/views/$fxmlFileName")

  private def newFxmlControllerComponent[N <: Node, C](fxmlFileName: String): (N, C) = {
    val fxmlLoader = new FXMLLoader(resource(fxmlFileName))
    val view: N = fxmlLoader.load()
    val ctrl: C = fxmlLoader.getController
    (view, ctrl)
  }

  def newRootComponent: (VBox, MainViewController) = newFxmlControllerComponent[VBox, MainViewController]("main-view.fxml")

  def newServerListCell: ListCell[DiscoveryService.ClientName] = new ServerListCell()
}
