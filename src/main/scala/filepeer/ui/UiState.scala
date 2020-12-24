package filepeer.ui

import filepeer.core.Env
import filepeer.core.discovery.DiscoveryService
import filepeer.core.transfer.FileReceiver
import javafx.beans.Observable
import javafx.beans.property.{ObjectProperty, SimpleListProperty, SimpleObjectProperty}
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections

class UiState(val env:Env) {

  private val availableServersProp = new SimpleObjectProperty[Set[DiscoveryService.ClientName]]()
  private val fileSavedProp = new SimpleObjectProperty[FileReceiver.FileSaved]()

  val discoverySubscriber: DiscoveryService.DiscoverySubscriber = new DiscoveryService.DiscoverySubscriber {
    override def newClient(client: DiscoveryService.ClientName,
                           allClients: Set[DiscoveryService.ClientName]): Unit = availableServersProp.setValue(allClients)
  }

  val fileSavedSubscriber: FileReceiver.FileSavedObserver = new FileReceiver.FileSavedObserver {
    override def fileSaved(file: FileReceiver.FileSaved): Unit = fileSavedProp.setValue(file)
  }

  def availableServers$:ObservableValue[Set[DiscoveryService.ClientName]] = availableServersProp
  def fileSaved$:ObservableValue[FileReceiver.FileSaved] = fileSavedProp
}
