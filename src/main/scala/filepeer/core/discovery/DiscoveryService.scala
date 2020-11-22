package filepeer.core.discovery

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}

class DiscoveryService(actorSystem: ActorSystem) {
  private val discoveryManager = actorSystem.actorOf(Props(classOf[DiscoveryManager]), "discovery-manager")
}

private class DiscoveryManager extends Actor with ActorLogging {

  private val listeningActor = context.system.actorOf(DiscoveryListeningActor.props, DiscoveryListeningActor.actorName)
  private val sendingActor = context.system.actorOf(DiscoverySendingActor.props, DiscoverySendingActor.actorName)

  override def receive: Receive = {
    case x => log.warning("unhandled: {}", x)
  }
}
