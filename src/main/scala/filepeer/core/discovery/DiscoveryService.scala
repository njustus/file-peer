package filepeer.core.discovery

import java.time.Instant

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import filepeer.core.{Address, Env, discovery}

import scala.collection.mutable

private class DiscoveryManager(subscriber:DiscoveryService.DiscoveryObserver, env: Env) extends Actor with ActorLogging {

  private val listeningActor = context.system.actorOf(DiscoveryListeningActor.props(self, env.discovery), DiscoveryListeningActor.actorName)
  private val sendingActor = context.system.actorOf(DiscoverySendingActor.props(env), DiscoverySendingActor.actorName)

  private val clients = mutable.LinkedHashSet.empty[discovery.DiscoveryService.ClientName]

  override def receive: Receive = {
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
  case class ClientName(hostName: String, ip: String, port: Int, discoveredAt:Instant = Instant.now) {
    def isLocalhost: Boolean = hostName == DiscoverySendingActor.hostSystem

    def address: Address = Address(ip, port)

    override def equals(obj: Any): Boolean = obj match {
      case ClientName(_, ip, port, _) => this.ip == ip
      case _ => false
    }

    override def hashCode(): Int = this.ip.hashCode
  }

  trait DiscoveryObserver {
    def newClient(client:ClientName, allClients:Set[ClientName]): Unit
  }

  private[core] def create(subscriber:DiscoveryService.DiscoveryObserver)(implicit actorSystem: ActorSystem, env: Env): ActorRef = {
    actorSystem.actorOf(Props(classOf[DiscoveryManager], subscriber, env), "discovery-manager")
  }
}
