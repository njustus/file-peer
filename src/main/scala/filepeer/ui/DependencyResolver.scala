package filepeer.ui

import com.typesafe.scalalogging.LazyLogging
import filepeer.core.{BackendModule, Env}
import filepeer.ui.components.ComponentFactory
import filepeer.ui.state.UiState

class DependencyResolver(uiState: UiState, backend: BackendModule)(implicit env: Env) extends LazyLogging {

  private val knownInstances = Set(env, backend.fileSender)
  private val clazzToInstances = knownInstances.map(x => (x.getClass, x)).toMap

  logger.debug(s"known classes: ${clazzToInstances.keys}")

  def getBean[A <: Object](clazz:Class[A]): A = {
    val instance = clazzToInstances.get(clazz)

    instance.map(clazz.cast)
      .getOrElse(throw new IllegalArgumentException(s"No instance for required bean of type: '${clazz.getCanonicalName}' found!"))
  }
}
