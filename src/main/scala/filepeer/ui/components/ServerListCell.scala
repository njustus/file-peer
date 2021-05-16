package filepeer.ui.components

import filepeer.core.discovery.DiscoveryService
import javafx.scene.control.{Label, ListCell}
import javafx.scene.layout.{HBox, VBox}
import org.controlsfx.glyphfont.{FontAwesome, Glyph}

private[components] class ServerListCell() extends ListCell[DiscoveryService.ClientName] {

  private val rootNode = new HBox()

  private val textRootNode = new VBox()
  private val nameLbl = new Label()
  private val addressLbl = new Label()
  private val icon = new Glyph("FontAwesome", FontAwesome.Glyph.LAPTOP)

  rootNode.getStyleClass.add("serverlist-cell-wrapper")
  nameLbl.getStyleClass.addAll("serverlist-cell", "serverlist-cell-name")
  addressLbl.getStyleClass.addAll("serverlist-cell", "serverlist-cell-address")

  rootNode.getChildren.addAll(icon, textRootNode)
  textRootNode.getChildren.addAll(nameLbl, addressLbl)

  override protected def updateItem(clientName: DiscoveryService.ClientName, empty: Boolean): Unit = {
    super.updateItem(clientName, empty)

    if(empty || clientName == null) {
      setGraphic(null)
    } else {
      nameLbl.setText(clientName.hostName)
      addressLbl.setText(clientName.ip)

      if(clientName.isLocalhost) {
        this.getStyleClass().add("serverlist-cell--localhost")
        icon.setIcon(FontAwesome.Glyph.HOME)
      }

      setGraphic(rootNode)
    }
  }
}
