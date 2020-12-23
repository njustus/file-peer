package filepeer.ui

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

class FilePeerUi extends Application {
  override def start(primaryStage:Stage):Unit = {
    primaryStage.setTitle("Hello World!");
    val btn = new Button();
    btn.setText("Say 'Hello World'");
    btn.setOnAction(event => println("Hello World!"))

    val root = new StackPane();
    root.getChildren().add(btn);
    primaryStage.setScene(new Scene(root, 300, 250));
    primaryStage.show();
  }
}

object FilePeerUi {
  def main(args: Array[String]) = {
    Application.launch(classOf[FilePeerUi], args:_*)
  }
}
