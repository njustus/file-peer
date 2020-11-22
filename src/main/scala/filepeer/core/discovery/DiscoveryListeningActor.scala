package filepeer.core.discovery

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.{IO, Udp}
import io.circe.generic.JsonCodec

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
      val payload = data.utf8String
      log.debug("new msg from {}, payload: {}", remote.getAddress, payload)
  }
}

object DiscoveryListeningActor {
  def props: Props = Props(classOf[DiscoveryListeningActor])
  val actorName: String = "discovery-listener"

  @JsonCodec
  case class ClientAddress(host: String, port: Int)
}
