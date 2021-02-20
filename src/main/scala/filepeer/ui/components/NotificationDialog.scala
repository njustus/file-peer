package filepeer.ui.components

import javafx.scene.control.Alert
import javafx.scene.control.Alert.AlertType

object NotificationDialog {
  def newDialog(msg: String): Alert = new Alert(AlertType.INFORMATION, msg)

  def notifyAndWait(msg: String): Unit = newDialog(msg).showAndWait()
}
