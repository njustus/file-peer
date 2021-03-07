package filepeer.ui.bootstrap

import filepeer.core.{BackendModule, Env, PureConfigSupport}
import filepeer.ui.DependencyResolver
import filepeer.ui.state.UiState
import javafx.application.Application
import javafx.scene.Scene
import javafx.stage.Stage
import pureconfig.ConfigSource

private[bootstrap] class FilePeerUi
  extends Application
    with PureConfigSupport {

  override def start(primaryStage:Stage):Unit = {
    import pureconfig.generic.auto._
    implicit val env: Env = ConfigSource.default.at("file-peer").loadOrThrow[Env]
    val state = new UiState(env)
    val backend = new BackendModule(state.discoverySubscriber, state.fileSavedSubscriber)
    val resolver = new DependencyResolver(state, backend)

    val (mainView, controller) = resolver.componentFactory.newRootComponent
    val subscriptions = controller.connectUiState(state)

    backend.http.bind(env.transfer.address)

    val scene = new Scene(mainView, 600, 400)
    primaryStage.setScene(scene)

    primaryStage.setTitle("FilePeer")
    primaryStage.setOnCloseRequest(ev => {
      backend.system.terminate()
      subscriptions.unsubscribe()
    })
    primaryStage.show()
  }

}
