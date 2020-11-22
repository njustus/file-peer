package filepeer.core.discovery

import java.net.{InetAddress, InetSocketAddress}

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.{IO, Inet, Udp}
import akka.util.ByteString
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class DiscoverySendingActor  extends Actor with ActorLogging {
  import context.system

  val broadcastAddress = new InetSocketAddress("255.255.255.255", 8071)
  IO(Udp) ! Udp.SimpleSender(Seq(Udp.SO.Broadcast(true)))

  context.system.scheduler.scheduleAtFixedRate(5 seconds, 5 seconds, self, DiscoverySendingActor.Broadcast)(context.system.dispatcher)

  override def receive: Receive = {
    case Udp.SimpleSenderReady =>
      log.info("got udp sender: {}", sender())
      context.become(discover(sender()))
  }

  private def discover(socket: ActorRef): Receive = {
    case DiscoverySendingActor.Broadcast =>

      val payload = DiscoveryListeningActor.ClientAddress(InetAddress.getLocalHost.getHostName, 8075)
      val msg = ByteString(payload.asJson.noSpaces)
      log.debug("sending broadcast..")
      socket ! Udp.Send(msg, broadcastAddress)
  }
}

object DiscoverySendingActor {
  def props: Props = Props(classOf[DiscoverySendingActor])
  val actorName: String = "discovery-sender"

  sealed trait DiscoveryMsg
  case object Broadcast extends DiscoveryMsg
}
