package filepeer.ui

import filepeer.core.{BackendModule, Env}
import javafx.application.Application
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.layout.{Pane, StackPane, VBox}
import javafx.stage.Stage
import pureconfig.ConfigSource

class FilePeerUi extends Application {
  override def start(primaryStage:Stage):Unit = {
    import pureconfig.generic.auto._
    implicit val env = ConfigSource.default.at("file-peer").loadOrThrow[Env]
    val state = new UiState(env)

    val (mainView, controller) = loadRootFxml(state)
    val scene = new Scene(mainView, 600, 400)
    scene.getStylesheets.add("style.css")
    primaryStage.setScene(scene)


    val backend = new BackendModule(state.discoverySubscriber, state.fileSavedSubscriber)

    primaryStage.setTitle("FilePeer")
    primaryStage.setOnCloseRequest(ev => backend.system.terminate())
    primaryStage.show();
  }

  private def loadRootFxml(state: UiState): (Pane, MainViewController) = {
    val (mainView, ctrl) = ComponentFactory.newRootComponent
    ctrl.connectUiState(state)
    (mainView, ctrl)
  }
}

object FilePeerUi {
  def main(args: Array[String]) = {
    Application.launch(classOf[FilePeerUi], args:_*)
  }
}
