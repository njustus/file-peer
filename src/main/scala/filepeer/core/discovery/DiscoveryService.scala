package filepeer.core.discovery

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import filepeer.core.{Address, DiscoveryEnv, Env, discovery}

import scala.collection.mutable

class DiscoveryService(subscriber:DiscoveryService.DiscoveryObserver)(implicit actorSystem: ActorSystem, env: Env) {
  private val discoveryManager = actorSystem.actorOf(Props(classOf[DiscoveryManager], subscriber, env), "discovery-manager")
}

private class DiscoveryManager(subscriber:DiscoveryService.DiscoveryObserver, env: Env) extends Actor with ActorLogging {

  private val listeningActor = context.system.actorOf(DiscoveryListeningActor.props(self, env.discovery), DiscoveryListeningActor.actorName)
  private val sendingActor = context.system.actorOf(DiscoverySendingActor.props(env), DiscoverySendingActor.actorName)

  private val clients = mutable.LinkedHashSet.empty[discovery.DiscoveryService.ClientName]

  override def receive: Receive = {
    case client: DiscoveryService.ClientName =>
      log.info("discovered new client: {}", client)
      clients += client
      subscriber.newClient(client, clients.toSet)
  }
}

object DiscoveryService {
  case class ClientName(hostName: String, ip: String, port: Int) {
    def address: Address = Address(ip, port)
    override def equals(obj: Any): Boolean = obj match {
      case ClientName(_, ip, port) => this.ip == ip
      case _ => false
    }
  }

  trait DiscoveryObserver {
    def newClient(client:ClientName, allClients:Set[ClientName]): Unit
  }
}
