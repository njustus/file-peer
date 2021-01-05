package filepeer.ui.state

import filepeer.core.Env
import filepeer.core.discovery.DiscoveryService
import filepeer.core.transfer.FileReceiver
import filepeer.ui.state.actions._
import rx.lang.scala.Observable

class UiState(val env:Env) {

  private val state = ReduxState[FPState, StateAction](FPState.zero)(UiState.stateReducer)

  val discoverySubscriber: DiscoveryService.DiscoveryObserver = new DiscoveryService.DiscoveryObserver {
    override def newClient(client: DiscoveryService.ClientName,
      allClients: Set[DiscoveryService.ClientName]): Unit = {
      val clients = allClients.toList.sortBy(_.hostName)
      dispatchAction(UpdateAvailableClients(clients))
    }
  }

  val fileSavedSubscriber: FileReceiver.FileSavedObserver = new FileReceiver.FileSavedObserver {
    override def fileSaved(file: FileReceiver.FileSaved): Unit = dispatchAction(FileSavedAction(file))
  }

  def availableServers$: Observable[List[DiscoveryService.ClientName]] = state.state$.map(_.availableClients).distinctUntilChanged
  def currentClient$: Observable[DiscoveryService.ClientName] = state.state$.map(_.currentClient).collect { case Some(client) => client}.distinctUntilChanged

  def dispatchAction: StateAction => Unit = state.dispatchAction
}

object UiState {
  def apply()(implicit env:Env): UiState = new UiState(env)

  private def stateReducer(state: FPState, action: StateAction): FPState = action match {
    case UpdateAvailableClients(clients) => state.copy(availableClients = clients)
    case UpdateCurrentClient(client) => state.copy(currentClient = Some(client))
    case FileSavedAction(fileSaved) => state.copy(fileSaved = Some(fileSaved))
  }
}
