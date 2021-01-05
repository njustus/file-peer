package filepeer.ui

import com.typesafe.scalalogging.LazyLogging
import filepeer.FilePeerTestSuite
import filepeer.core.{ActorTestSuite, BackendModule, Env}
import filepeer.core.discovery.DiscoveryService
import filepeer.core.discovery.DiscoveryService.DiscoveryObserver
import filepeer.core.transfer.{Client, FileReceiver}
import filepeer.core.transfer.FileReceiver.FileSavedObserver

class DependencyResolverSuite extends ActorTestSuite with LazyLogging {
  private val discoveryObserver = new DiscoveryObserver {
    override def newClient(client: DiscoveryService.ClientName, allClients: Set[DiscoveryService.ClientName]): Unit = logger.warn(s"unhandled new client message received: $client")
  }
  private val fileObserver = new FileSavedObserver {
    override def fileSaved(file: FileReceiver.FileSaved): Unit = logger.warn(s"unhandled file saved message received: $file")
  }

  val backendModule = new BackendModule(discoveryObserver, fileObserver)
  val resolver = new DependencyResolver(backendModule)

  "The 'DependencyResolver'" should "resolve a service by its class symbol" in {
    val client = resolver.getBean(classOf[Client])
    client shouldBe a [Client]
  }
  it should "resolve the env configuration" in {
    val resolvedEnv = resolver.getBean(classOf[Env])
    resolvedEnv shouldBe a [Env]
    resolvedEnv shouldBe (env)
  }

  it should "throw an exception for unknown classes" in {
    an [IllegalArgumentException] shouldBe thrownBy(resolver.getBean(classOf[FilePeerTestSuite]))
  }
}
