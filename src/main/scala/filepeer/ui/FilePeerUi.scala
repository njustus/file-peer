package filepeer.ui

import javafx.application.Application
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.layout.{StackPane, VBox}
import javafx.stage.Stage;

class FilePeerUi extends Application {
  override def start(primaryStage:Stage):Unit = {
    primaryStage.setTitle("FilePeer")

    val mainView:VBox = FXMLLoader.load(getClass.getResource("/views/main-view.fxml"))
    primaryStage.setScene(new Scene(mainView, 600, 400));
    primaryStage.show();
  }
}

object FilePeerUi {
  def main(args: Array[String]) = {
    Application.launch(classOf[FilePeerUi], args:_*)
  }
}
