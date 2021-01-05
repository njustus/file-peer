package filepeer.core

import akka.actor.ActorSystem
import akka.stream.Materializer
import filepeer.FilePeerTestSuite

import scala.concurrent.duration._

abstract class ActorTestSuite extends FilePeerTestSuite {
  implicit val system = ActorSystem("filepeer-testsystem")
  implicit val mat = Materializer.matFromSystem
  implicit val exec = system.dispatcher

  implicit val testTimeout = 20 seconds
  override def afterAll() = {
    system.terminate()
  }
}
