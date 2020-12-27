package filepeer.ui.state

import filepeer.core.discovery.DiscoveryService
import filepeer.core.transfer.FileReceiver.FileSaved

object actions {
  sealed trait StateAction
  case class UpdateAvailableClients(clients:List[DiscoveryService.ClientName]) extends StateAction
  case class UpdateCurrentClient(client:DiscoveryService.ClientName) extends StateAction
  case class FileSavedAction(fileSaved: FileSaved) extends StateAction
}
