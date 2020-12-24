package filepeer.core.discovery

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import filepeer.core.{DiscoveryEnv, Env, discovery}

import scala.collection.mutable

class DiscoveryService(subscriber:DiscoveryManager.DiscoverySubscriber)(implicit actorSystem: ActorSystem, env: Env) {
  private val discoveryManager = actorSystem.actorOf(Props(classOf[DiscoveryManager], subscriber, env), "discovery-manager")
}

private class DiscoveryManager(subscriber:DiscoveryManager.DiscoverySubscriber, env: Env) extends Actor with ActorLogging {

  private val listeningActor = context.system.actorOf(DiscoveryListeningActor.props(self, env.discovery), DiscoveryListeningActor.actorName)
  private val sendingActor = context.system.actorOf(DiscoverySendingActor.props(env), DiscoverySendingActor.actorName)

  private val clients = mutable.LinkedHashSet.empty[discovery.DiscoveryManager.ClientName]

  override def receive: Receive = {
    case client: DiscoveryManager.ClientName =>
      log.info("discovered new client: {}", client)
      clients += client
      subscriber.newClient(client, clients.toSet)
  }
}

object DiscoveryManager {
  case class ClientName(hostName: String, ip: String, port: Int)

  trait DiscoverySubscriber {
    def newClient(client:ClientName, allClients:Set[ClientName]): Unit
  }
}
