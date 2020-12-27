package filepeer.ui.components

import filepeer.core.discovery.DiscoveryService
import javafx.scene.control.{Label, ListCell}
import javafx.scene.layout.VBox

private[components] class ServerListCell() extends ListCell[DiscoveryService.ClientName] {

  private val rootNode = new VBox()
  private val nameLbl = new Label()
  private val addressLbl = new Label()

  nameLbl.getStyleClass.addAll("serverlist-cell", "serverlist-cell-name")
  addressLbl.getStyleClass.addAll("serverlist-cell", "serverlist-cell-address")

  rootNode.getChildren.addAll(nameLbl, addressLbl)

  override protected def updateItem(clientName: DiscoveryService.ClientName, empty: Boolean): Unit = {
    super.updateItem(clientName, empty)

    if(empty || clientName == null) {
      setGraphic(null)
    } else {
      nameLbl.setText(clientName.hostName)
      addressLbl.setText(clientName.ip)

      setGraphic(rootNode)
    }
  }
}
