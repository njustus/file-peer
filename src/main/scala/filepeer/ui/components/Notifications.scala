package filepeer.ui.components

import javafx.stage.Window
import javafx.util.Duration
import org.controlsfx.control.{Notifications => Notificator}

trait Notifications {

  def notify(msg: String, title: String = ""): Unit = {
    Notificator.create()
      .owner(currentWindow)
      .title(title)
      .hideAfter(Duration.seconds(5))
      .text(msg)
      .showInformation()
  }

  protected def currentWindow: Window
}
