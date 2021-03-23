package filepeer.ui.state

import filepeer.core.Env
import filepeer.core.discovery.DiscoveryService
import filepeer.core.transfer.FileReceiver
import filepeer.ui.components.TransferConfirmationDialog
import filepeer.ui.state.actions._
import javafx.application.Platform
import rx.lang.scala.Observable

import scala.concurrent.{Future, Promise}

class UiState(val env:Env) {

  private val state = ReduxState[FPState, StateAction](FPState.zero)(UiState.stateReducer)

  val discoverySubscriber: DiscoveryService.DiscoveryObserver = new DiscoveryService.DiscoveryObserver {
    private def updateClients(allClients:Set[DiscoveryService.ClientName]): Unit = {
      val clients = allClients.toList.sortBy(_.hostName)
      dispatchAction(UpdateAvailableClients(clients))
    }


    override def newClient(client: DiscoveryService.ClientName, allClients: Set[DiscoveryService.ClientName]): Unit = {
      updateClients(allClients)
    }

    override def goneClients(client:Set[DiscoveryService.ClientName], allClients:Set[DiscoveryService.ClientName]): Unit = {
      updateClients(allClients)
    }
  }

  val fileSavedSubscriber: FileReceiver.FileSavedObserver = new FileReceiver.FileSavedObserver {
    override def accept(file: FileReceiver.FileSaved): Future[Boolean] = {
      // the accept() method is called from within an Akka Flow,
      // but the confirmation dialog must run in the JFX thread
      val acceptPromise = Promise[Boolean]()
      Platform.runLater { () =>
        val accepted = TransferConfirmationDialog.displayDialog(file)
        acceptPromise.success(accepted)
      }
      acceptPromise.future
    }

    override def fileSaved(file: FileReceiver.FileSaved): Unit = dispatchAction(FileSavedAction(file))
  }

  def availableServers$: Observable[List[DiscoveryService.ClientName]] = state.state$.map(_.availableClients).distinctUntilChanged
  def currentClient$: Observable[DiscoveryService.ClientName] = state.state$.map(_.currentClient).collect { case Some(client) => client}.distinctUntilChanged
  def filSaved$: Observable[FileReceiver.FileSaved] = state.state$.map(_.fileSaved).collect  { case Some(fs) => fs }.distinctUntilChanged

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
