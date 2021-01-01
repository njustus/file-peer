package filepeer.ui

import filepeer.core.{BackendModule, Env}
import filepeer.ui.components.ComponentFactory
import filepeer.ui.state.UiState
import javafx.application.Application
import javafx.scene.Scene
import javafx.stage.Stage
import pureconfig.ConfigSource

class FilePeerUi extends Application {
  override def start(primaryStage:Stage):Unit = {
    import pureconfig.generic.auto._
    implicit val env = ConfigSource.default.at("file-peer").loadOrThrow[Env]
    val state = new UiState(env)
    val backend = new BackendModule(state.discoverySubscriber, state.fileSavedSubscriber)

    val (mainView, controller) = ComponentFactory.newRootComponent
    controller.setFileSender(backend.fileSender)
    val subscriptions = controller.connectUiState(state)

    val scene = new Scene(mainView, 600, 400)
    primaryStage.setScene(scene)

    primaryStage.setTitle("FilePeer")
    primaryStage.setOnCloseRequest(ev => {
      backend.system.terminate()
      subscriptions.unsubscribe()
    })
    primaryStage.show();
  }

}

object FilePeerUi {
  def main(args: Array[String]): Unit = {
    Application.launch(classOf[FilePeerUi], args:_*)
  }
}
