package filepeer.core.discovery

import java.net.InetAddress

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.{IO, Udp}
import akka.util.ByteString
import filepeer.core.Env
import io.circe.syntax._

private[discovery] class DiscoverySendingActor(env: Env)  extends Actor with ActorLogging {
  import context.system
  import context.dispatcher

  private val broadcastAddress = env.discovery.broadcastAddress
  log.info(s"broadcasting on ${broadcastAddress}")

  IO(Udp) ! Udp.SimpleSender(Seq(Udp.SO.Broadcast(true)))

  context.system.scheduler.scheduleAtFixedRate(env.discovery.broadcastInterval,
                                                env.discovery.broadcastInterval,
                                                self,
                                                DiscoverySendingActor.Broadcast)

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

private[discovery] object DiscoverySendingActor {
  def props(env: Env): Props = Props(classOf[DiscoverySendingActor], env)
  val actorName: String = "discovery-sender"
  val hostSystem: String = InetAddress.getLocalHost.getHostName

  sealed trait DiscoveryMsg
  case object Broadcast extends DiscoveryMsg
}
