package filepeer.core.discovery

import filepeer.core.ActorTestSuite
import filepeer.core.discovery.DiscoveryService.ClientName
import rx.lang.scala.Subject

import scala.concurrent.Promise

class DiscoverySpec extends ActorTestSuite {

  private val discoveredClients$ = Subject[ClientName]()

  private val discovery = DiscoveryService.create(
    (client: ClientName, allClients: Set[ClientName]) => discoveredClients$.onNext(client)
  )

  override def afterAll(): Unit = {
    super.afterAll()
    discoveredClients$.onCompleted()
  }

  "The discovery feature" should "discover running servers" in {
    val firstClient = Promise[ClientName]()
    discoveredClients$.first.subscribe(x => firstClient.success(x))

    firstClient.future.map { client =>
      client.hostName shouldBe (DiscoverySendingActor.hostSystem)
      client.port shouldBe (env.transfer.address.port)
    }
  }
}
