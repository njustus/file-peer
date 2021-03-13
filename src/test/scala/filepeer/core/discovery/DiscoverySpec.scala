package filepeer.core.discovery

import filepeer.core.ActorTestSuite
import filepeer.core.discovery.DiscoveryService.ClientName
import rx.lang.scala.Subject

import scala.concurrent.Promise
import akka.actor.PoisonPill

class DiscoverySpec extends ActorTestSuite {

  private val discoveredClients$ = Subject[ClientName]()
  private val removedClients$ = Subject[ClientName]()

  private val discovery = DiscoveryService.create(new DiscoveryService.DiscoveryObserver {
    override def newClient(client: ClientName, allClients: Set[ClientName]) =
      discoveredClients$.onNext(client)

    override def goneClients(client:Set[ClientName], allClients:Set[ClientName]): Unit =
      client.foreach(c => removedClients$.onNext(c))
  })

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

  it should "remove obsolete clients" in {
    val actor = selectActor(s"/user/${DiscoveryListeningActor.actorName}")
    actor ! PoisonPill

    val removedClient = Promise[ClientName]()
    removedClients$.first.subscribe(x => removedClient.success(x))

    removedClient.future.map { client =>
      client.hostName shouldBe (DiscoverySendingActor.hostSystem)
      client.port shouldBe (env.transfer.address.port)
    }
  }
}
