package filepeer.ui

import javafx.beans.value.ChangeListener
import scala.language.implicitConversions

trait CallbackImplicits {
  implicit def toChangeListener[A](fn: A => Unit): ChangeListener[A] = (observable, oldValue, newValue) => {
    if(newValue != null) {
      fn(newValue)
    }
  }
}
