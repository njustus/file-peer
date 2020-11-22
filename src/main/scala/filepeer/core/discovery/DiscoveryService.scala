package filepeer.core.discovery

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import filepeer.core.discovery

import scala.collection.mutable

class DiscoveryService(actorSystem: ActorSystem) {
  private val discoveryManager = actorSystem.actorOf(Props(classOf[DiscoveryManager]), "discovery-manager")
}

private class DiscoveryManager extends Actor with ActorLogging {

  private val listeningActor = context.system.actorOf(DiscoveryListeningActor.props(self), DiscoveryListeningActor.actorName)
  private val sendingActor = context.system.actorOf(DiscoverySendingActor.props, DiscoverySendingActor.actorName)

  private val clients = mutable.LinkedHashSet.empty[discovery.DiscoveryManager.ClientName]

  override def receive: Receive = {
    case client: DiscoveryManager.ClientName =>
      log.info("discovered new client: {}", client)
      clients += client
  }
}

object DiscoveryManager {
  case class ClientName(hostName: String, ip: String, port: Int)
}
