package filepeer.core.discovery

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}

import scala.collection.mutable

class DiscoveryService(actorSystem: ActorSystem) {
  private val discoveryManager = actorSystem.actorOf(Props(classOf[DiscoveryManager]), "discovery-manager")
}

private class DiscoveryManager extends Actor with ActorLogging {

  private val listeningActor = context.system.actorOf(DiscoveryListeningActor.props(self), DiscoveryListeningActor.actorName)
  private val sendingActor = context.system.actorOf(DiscoverySendingActor.props, DiscoverySendingActor.actorName)

  private val clients = mutable.ArrayBuffer.empty[DiscoveryListeningActor.ClientAddress]

  override def receive: Receive = {
    case client: DiscoveryListeningActor.ClientAddress =>
      log.debug("discovered new client: {}", client)
      clients += client
  }
}
