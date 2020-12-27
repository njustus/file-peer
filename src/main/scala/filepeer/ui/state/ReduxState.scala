package filepeer.ui.state

import rx.lang.scala.{Observable, Subject}

private[state] case class ReduxState[S, A](zero:S)(reducer: (S, A) => S) {
  private val actions = Subject[A]()

  val dispatchAction: A => Unit = actions.onNext
  val state$: Observable[S] = actions.scan(zero)(reducer).share
}
