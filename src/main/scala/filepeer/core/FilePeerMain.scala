package filepeer.core

import akka.actor.ActorSystem
import akka.io.Udp
import filepeer.core.discovery.DiscoveryService

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object FilePeerMain {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem("file-peer")
    scala.sys.addShutdownHook {
      system.terminate()
    }

    val discoveryService = new DiscoveryService(system)


    Await.ready(system.whenTerminated, Duration.Inf)
  }
}
