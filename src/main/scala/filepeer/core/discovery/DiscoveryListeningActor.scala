package filepeer.core.discovery

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.{IO, Udp}
import akka.util.ByteString
import filepeer.core.discovery.DiscoveryListeningActor.ClientAddress
import io.circe.generic.JsonCodec
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._

class DiscoveryListeningActor(interestee: ActorRef, discovery:filepeer.core.DiscoveryEnv) extends Actor with ActorLogging {
  import context.system

  val listeningSocketAddress = new InetSocketAddress("0.0.0.0", discovery.address.port)

  IO(Udp) ! Udp.Bind(self, listeningSocketAddress)

  override def receive: Receive = {
    case Udp.Bound(localAddress) =>
      log.info("Listener connected to: {}:{}", localAddress.getHostString, localAddress.getPort)
      context.become(up(sender()))
  }

  def up(socket: ActorRef): Receive = {
    case Udp.Received(data, remote) =>
      decodeAndPublishClient(data, remote)
  }

  private def decodeAndPublishClient(msg: ByteString, addr: InetSocketAddress): Unit = {
    val payload = msg.utf8String
    log.debug("new msg from {}, payload: {}", addr.getAddress, payload)
    decode[ClientAddress](payload).toTry
      .filter {
        case addr if !discovery.includeLocalhost => addr.host != DiscoverySendingActor.hostSystem //ignore myself
        case _ => true
      }
      .foreach { case ClientAddress(hostName, port) =>
      interestee ! DiscoveryService.ClientName(hostName, addr.getHostName, port)
    }
  }
}

object DiscoveryListeningActor {
  def props(interestee: ActorRef, discovery:filepeer.core.DiscoveryEnv): Props = Props(classOf[DiscoveryListeningActor], interestee, discovery)
  val actorName: String = "discovery-listener"

  @JsonCodec
  case class ClientAddress(host: String, port: Int)
}
