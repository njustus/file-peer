package filepeer.core

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.typesafe.scalalogging.LazyLogging
import filepeer.core.discovery.DiscoveryService
import filepeer.core.discovery.DiscoveryService.DiscoveryObserver
import filepeer.core.transfer.FileReceiver.FileSavedObserver
import filepeer.core.transfer.{FileReceiver, HttpClient, HttpReceiver}

class BackendModule(discoverySubscriber: DiscoveryObserver,
                    receiverSubscriber: FileSavedObserver)(implicit env: Env,
                                                           val system: ActorSystem = ActorSystem("file-peer"))
  extends LazyLogging {
  logger.info(s"""enabled features: [${env.features.mkString(",")}]""")

  FileReceiver.createTargetDir(env.transfer)

  implicit val mat = Materializer.matFromSystem
  scala.sys.addShutdownHook {
    system.terminate()
  }


  private val discoveryServiceActorOpt =
    if (env.discoveryFeatureIsEnabled) {
      Some(DiscoveryService.create(discoverySubscriber))
    } else {
      None
    }

  val fileReceiver = new FileReceiver(receiverSubscriber)
  val http = new HttpReceiver(fileReceiver)
//  val transferServer = new TransferServer(fileReceiver)
  val fileSender = new HttpClient()
}
