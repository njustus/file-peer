package filepeer.core.discovery

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.{IO, Udp}

class DiscoveryListeningActor extends Actor with ActorLogging {
  import context.system

  val listeningSocketAddress = new InetSocketAddress("0.0.0.0", 8071)

  IO(Udp) ! Udp.Bind(self, listeningSocketAddress)

  override def receive: Receive = {
    case Udp.Bound(localAddress) =>
      log.info("Listener connected to: {}:{}", localAddress.getHostString, localAddress.getPort)
      context.become(up(sender()))
  }

  def up(socket: ActorRef): Receive = {
    case Udp.Received(data, remote) =>
      log.debug("received: {} bytes from {}", data.length, remote.getAddress)
  }
}

object DiscoveryListeningActor {
  def props: Props = Props(classOf[DiscoveryListeningActor])
  val actorName: String = "discovery-listener"
}
