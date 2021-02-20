package filepeer.ui

import com.typesafe.scalalogging.LazyLogging
import filepeer.FilePeerTestSuite
import filepeer.core.{ActorTestSuite, BackendModule, Env}
import filepeer.core.discovery.DiscoveryService
import filepeer.core.discovery.DiscoveryService.DiscoveryObserver
import filepeer.core.transfer.{Client, FileReceiver, HttpClient}
import filepeer.core.transfer.FileReceiver.FileSavedObserver
import filepeer.ui.DependencyResolverSuite.{DummyClass, DummyController}
import filepeer.ui.state.UiState

class DependencyResolverSuite extends ActorTestSuite with LazyLogging {
  private val discoveryObserver = new DiscoveryObserver {
    override def newClient(client: DiscoveryService.ClientName, allClients: Set[DiscoveryService.ClientName]): Unit = logger.warn(s"unhandled new client message received: $client")
  }

  private val fileObserver = new FileSavedObserver {
    override def fileSaved(file: FileReceiver.FileSaved): Unit = logger.warn(s"unhandled file saved message received: $file")
  }

  val backendModule = new BackendModule(discoveryObserver, fileObserver)
  val resolver = new DependencyResolver(UiState(), backendModule)

  "The 'DependencyResolver'" should "resolve a service by its class symbol" in {
    val client = resolver.getBean(classOf[HttpClient])
    client shouldBe a [HttpClient]
  }
  it should "resolve the env configuration" in {
    val resolvedEnv = resolver.getBean(classOf[Env])
    resolvedEnv shouldBe a [Env]
    resolvedEnv shouldBe (env)
  }

  it should "throw an exception for unknown classes" in {
    an [IllegalArgumentException] shouldBe thrownBy(resolver.getBean(classOf[FilePeerTestSuite]))
  }

  it should "instantiate the main controller if it knows their dependents" in {
    val ctrl = resolver.getController(classOf[MainViewController])
    ctrl shouldBe a [MainViewController]
  }

  it should "instantiate a controller with 0 parameters" in {
    val ctrl = resolver.getController(classOf[DummyClass])
    ctrl shouldBe a [DummyClass]
  }
  it should "throw an exception if it doesn't know the dependents of a controller" in {
    an [IllegalArgumentException] shouldBe thrownBy(resolver.getController(classOf[DummyController]))
  }
}

object DependencyResolverSuite {
    class DummyClass() {}

    class DummyController(x:FilePeerTestSuite) {}
}
