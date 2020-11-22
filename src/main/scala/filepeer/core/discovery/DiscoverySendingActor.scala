package filepeer.core.discovery

import java.net.{InetAddress, InetSocketAddress}

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.{IO, Inet, Udp}
import akka.util.ByteString
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._

import scala.concurrent.duration._

class DiscoverySendingActor(env: filepeer.core.Env)  extends Actor with ActorLogging {
  import context.system

  val broadcastAddress = new InetSocketAddress("255.255.255.255", env.discovery.address.port)
  IO(Udp) ! Udp.SimpleSender(Seq(Udp.SO.Broadcast(true)))

  context.system.scheduler.scheduleAtFixedRate(5 seconds, 5 seconds, self, DiscoverySendingActor.Broadcast)(context.system.dispatcher)

  override def receive: Receive = {
    case Udp.SimpleSenderReady =>
      context.become(discover(sender()))
  }

  private def discover(socket: ActorRef): Receive = {
    case DiscoverySendingActor.Broadcast =>

      val payload = DiscoveryListeningActor.ClientAddress(DiscoverySendingActor.hostSystem, env.transfer.address.port)
      val msg = ByteString(payload.asJson.noSpaces)
      log.debug("sending broadcast..")
      socket ! Udp.Send(msg, broadcastAddress)
  }
}

object DiscoverySendingActor {
  def props(env: filepeer.core.Env): Props = Props(classOf[DiscoverySendingActor], env)
  val actorName: String = "discovery-sender"
  val hostSystem: String = InetAddress.getLocalHost.getHostName

  sealed trait DiscoveryMsg
  case object Broadcast extends DiscoveryMsg
}
