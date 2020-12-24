package filepeer.ui

import filepeer.core.{BackendModule, Env}
import javafx.application.Application
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.layout.{StackPane, VBox}
import javafx.stage.Stage
import pureconfig.ConfigSource;

class FilePeerUi extends Application {
  override def start(primaryStage:Stage):Unit = {
    import pureconfig.generic.auto._
    implicit val env = ConfigSource.default.at("file-peer").loadOrThrow[Env]
    primaryStage.setTitle("FilePeer")
    val mainView:VBox = FXMLLoader.load(this.getClass.getResource("/views/main-view.fxml"))
    primaryStage.setScene(new Scene(mainView, 600, 400));

    val state = new UiState(env)
    val backend = new BackendModule(state.discoverySubscriber, state.fileSavedSubscriber)

    state.availableServers$.addListener(servers => println("servers: ", servers))

    primaryStage.setOnCloseRequest(ev => backend.system.terminate())

    primaryStage.show();
  }
}

object FilePeerUi {
  def main(args: Array[String]) = {
    Application.launch(classOf[FilePeerUi], args:_*)
  }
}
