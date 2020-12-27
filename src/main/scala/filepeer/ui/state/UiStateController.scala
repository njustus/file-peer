package filepeer.ui.state

import rx.lang.scala.Subscription

trait UiStateController {
  def connectUiState(state: UiState): Subscription
}
