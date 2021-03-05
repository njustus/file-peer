package filepeer.core

import akka.actor.ActorSystem
import akka.stream.Materializer
import filepeer.core.discovery.DiscoveryService
import filepeer.core.discovery.DiscoveryService.DiscoveryObserver
import filepeer.core.transfer.FileReceiver.{FileSaved, FileSavedObserver}
import filepeer.core.transfer.{Client, FileReceiver, HttpClient, HttpReceiver, TransferServer}
import pureconfig.ConfigSource

class BackendModule(discoverySubscriber: DiscoveryObserver,
                    receiverSubscriber: FileSavedObserver)(implicit env: Env,
                                                           val system: ActorSystem = ActorSystem("file-peer")) {
    TransferServer.createTargetDir(env.transfer)

    implicit val mat = Materializer.matFromSystem
    scala.sys.addShutdownHook {
      system.terminate()
    }

  private val discoveryServiceActor = DiscoveryService.create(discoverySubscriber)

  val fileReceiver = new FileReceiver(receiverSubscriber)
  val http = new HttpReceiver(fileReceiver)
//  val transferServer = new TransferServer(fileReceiver)
  val fileSender = new HttpClient()
}
