package filepeer.core.discovery

import java.time.{Duration, Instant}
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import filepeer.core.{Address, Env, discovery}

import scala.collection.mutable
import scala.math.Ordering.Implicits.infixOrderingOps

private class DiscoveryManager(subscriber:DiscoveryService.DiscoveryObserver, env: Env) extends Actor with ActorLogging {
  import context.system
  import context.dispatcher

  private val listeningActor = system.actorOf(DiscoveryListeningActor.props(self, env.discovery), DiscoveryListeningActor.actorName)
  private val sendingActor = system.actorOf(DiscoverySendingActor.props(env), DiscoverySendingActor.actorName)

  private val clients = mutable.LinkedHashSet.empty[discovery.DiscoveryService.ClientName]

  system.scheduler.scheduleAtFixedRate(env.discovery.cleanupInterval,
                                        env.discovery.cleanupInterval,
                                        self,
                                        DiscoveryService.Cleanup)

  override def receive: Receive = {
    case DiscoveryService.Cleanup =>
      log.debug("cleaning up non-responding clients...")
      val now = Instant.now()
      val obsoleteClients = clients.filter(c => Duration.between(c.discoveredAt, now) > (Duration.ofSeconds(11)))

      log.debug(s"found ${obsoleteClients.size} clients to remove")
      clients --= obsoleteClients
      log.info(s"remaining ${clients.size} clients")
      subscriber.goneClients(obsoleteClients.toSet, clients.toSet)
    case client: DiscoveryService.ClientName =>
      if(clients contains client) {
        log.debug("rediscovered client: {}", client)
        clients -= client
        clients += client
      } else {
        log.info("discovered new client: {}", client)
        clients += client
      }

      log.debug(s"available clients: $clients")
      subscriber.newClient(client, clients.toSet)
  }
}

object DiscoveryService {
  case class ClientName private[discovery](hostName: String, ip: String, port: Int, discoveredAt:Instant = Instant.now) {
    def isLocalhost: Boolean = hostName == DiscoverySendingActor.hostSystem

    def address: Address = Address(ip, port)

    override def equals(obj: Any): Boolean = obj match {
      case other:ClientName => this.address == other.address
      case _ => false
    }

    override def hashCode(): Int = this.address.hashCode
  }

  private[discovery] case object Cleanup

  trait DiscoveryObserver {
    def newClient(client:ClientName, allClients:Set[ClientName]): Unit

    def goneClients(client:Set[ClientName], allClients:Set[ClientName]): Unit
  }

  private[core] def create(subscriber:DiscoveryService.DiscoveryObserver)(implicit actorSystem: ActorSystem, env: Env): ActorRef = {
    actorSystem.actorOf(Props(classOf[DiscoveryManager], subscriber, env), "discovery-manager")
  }
}
