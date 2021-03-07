package filepeer.ui.components

import filepeer.core.transfer.FileReceiver.FileSaved
import javafx.scene.control.Alert.AlertType
import javafx.scene.control.{Alert, ButtonType}

object TransferConfirmationDialog {

  def newDialog(file: FileSaved): Alert = {
    val text = s"Do you want to receive the file:\n${file.name}?"
    new Alert(AlertType.CONFIRMATION, text, ButtonType.YES, ButtonType.NO)
  }

  def displayDialog(file: FileSaved): Boolean = {
    val alert = newDialog(file)
    alert.showAndWait()
      .map(btn => btn == ButtonType.YES)
      .orElse(false)
  }
}
