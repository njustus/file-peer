package filepeer.ui

import filepeer.core.discovery.DiscoveryService
import filepeer.core.transfer.FileReceiver.FileSaved

package object state {
  case class FPState(
                    availableClients: List[DiscoveryService.ClientName],
                    currentClient: Option[DiscoveryService.ClientName],
                    fileSaved: Option[FileSaved])

  object FPState {
    def zero:FPState = FPState(List.empty, None, None)
  }
}
