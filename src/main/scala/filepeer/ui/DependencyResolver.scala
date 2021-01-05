package filepeer.ui

import com.typesafe.scalalogging.LazyLogging
import filepeer.core.{BackendModule, Env}
import filepeer.ui.components.ComponentFactory
import filepeer.ui.state.UiState

class DependencyResolver(uiState: UiState, backend: BackendModule)(implicit env: Env) extends LazyLogging {

  private val knownInstances = Set(uiState, env, backend.fileSender)
  private val clazzToInstances = knownInstances.map(x => (x.getClass, x)).toMap

  logger.debug(s"known classes: ${clazzToInstances.keys}")

  def getBean[A <: Object](clazz:Class[A]): A = {
    val instance = clazzToInstances.get(clazz)

    instance.map(clazz.cast)
      .getOrElse(throw new IllegalArgumentException(s"No instance for required bean of type: '${clazz.getCanonicalName}' found!"))
  }

  private def hasBean[A <: Object](clazz:Class[A]): Boolean = clazzToInstances.contains(clazz)

  def getController[A](clazz: Class[A]): A = {
    val constructorOpt = clazz.getDeclaredConstructors.find { cons =>
      cons.getParameterTypes.forall(paramClazz => hasBean(paramClazz))
    }

    val instance = constructorOpt.map { constructor =>
      logger.debug(s"found constructor for class: ${clazz.getCanonicalName}")

      if(constructor.getParameterCount > 0) {
        val params = constructor.getParameterTypes.map(cl => getBean(cl))
        constructor.newInstance(params:_*)
      } else {
        constructor.newInstance()
      }
    }
      .getOrElse(throw new IllegalArgumentException(s"Don't know how to instantiate controller of type: '${clazz.getCanonicalName}'!"))

    clazz.cast(instance)
  }
}
