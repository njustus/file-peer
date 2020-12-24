package filepeer.ui

import filepeer.core.Env
import filepeer.core.discovery.DiscoveryService
import filepeer.core.transfer.FileReceiver
import javafx.beans.Observable
import javafx.beans.property.{ObjectProperty, SimpleListProperty, SimpleObjectProperty}
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections

class UiState(val env:Env) {

  private val availableServersProp = new SimpleObjectProperty[List[DiscoveryService.ClientName]](List.empty)
  private val fileSavedProp = new SimpleObjectProperty[FileReceiver.FileSaved]()

  val discoverySubscriber: DiscoveryService.DiscoveryObserver = new DiscoveryService.DiscoveryObserver {
    override def newClient(client: DiscoveryService.ClientName,
      allClients: Set[DiscoveryService.ClientName]): Unit = {
      availableServersProp.setValue(allClients.toList)
    }
  }

  val fileSavedSubscriber: FileReceiver.FileSavedObserver = new FileReceiver.FileSavedObserver {
    override def fileSaved(file: FileReceiver.FileSaved): Unit = fileSavedProp.setValue(file)
  }

  def availableServers$:ObservableValue[List[DiscoveryService.ClientName]] = availableServersProp
  def fileSaved$:ObservableValue[FileReceiver.FileSaved] = fileSavedProp
}
