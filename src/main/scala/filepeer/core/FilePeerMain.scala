package filepeer.core

import akka.actor.ActorSystem
import akka.io.Udp
import filepeer.core.discovery.DiscoveryService

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import pureconfig._
import pureconfig.generic.auto._

object FilePeerMain {
  def main(args: Array[String]): Unit = {
    val env = ConfigSource.default.at("file-peer").loadOrThrow[Env]

    val system = ActorSystem("file-peer")
    scala.sys.addShutdownHook {
      system.terminate()
    }

    val discoveryService = new DiscoveryService(system, env)

    Await.ready(system.whenTerminated, Duration.Inf)
   }
}
