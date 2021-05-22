package filepeer.ui.components

import javafx.stage.Window
import javafx.util.Duration
import org.controlsfx.control.action.Action
import org.controlsfx.control.{Notifications => Notificator}

trait Notifications {

  def notifyA(actions: Action*)(msg: String, title: String = ""): Unit = {
    Notificator.create()
      .owner(currentWindow)
      .title(title)
      .hideAfter(Duration.seconds(5))
      .text(msg)
      .action(actions:_*)
      .showInformation()
  }

  def notify(msg: String, title: String = ""): Unit = notifyA()(msg, title)

  protected def currentWindow: Window
}
