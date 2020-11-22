package filepeer.core

import akka.actor.ActorSystem
import akka.io.Udp
import akka.stream.Materializer
import filepeer.core.discovery.DiscoveryService
import filepeer.core.transfer.TransferService

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import pureconfig._
import pureconfig.generic.auto._

object FilePeerMain {
  def main(args: Array[String]): Unit = {
    implicit val env = ConfigSource.default.at("file-peer").loadOrThrow[Env]

    implicit val system = ActorSystem("file-peer")
    implicit val mat = Materializer.matFromSystem
    scala.sys.addShutdownHook {
      system.terminate()
    }

    val discoveryService = new DiscoveryService()
    val transferService = new TransferService()

    Await.ready(system.whenTerminated, Duration.Inf)
   }
}
