package filepeer.ui.components

import filepeer.core.transfer.FileReceiver.FileSaved
import javafx.scene.control.{Alert, ButtonType}
import javafx.scene.control.Alert.AlertType

object TransferConfirmationDialog {

  def newDialog(file: FileSaved): Alert = {
    val text = s"Do you want to receive the file:\n${file.name}?"
    val alert = new Alert(AlertType.CONFIRMATION, text, ButtonType.YES, ButtonType.NO)
    alert
  }

  def displayDialog(file: FileSaved): Boolean = {
    val alert = newDialog(file)
    alert.showAndWait()
      .map(btn => btn == ButtonType.YES)
      .orElseGet(() => false)
  }
}
