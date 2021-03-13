package filepeer.core

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.testkit.TestKit
import filepeer.FilePeerTestSuite

import scala.concurrent.duration._

abstract class ActorTestSuite
  extends TestKit(ActorSystem("filepeer-testsystem"))
  with FilePeerTestSuite {

  implicit val mat = Materializer.matFromSystem
  implicit val exec = system.dispatcher

  implicit val testTimeout = 20 seconds

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  def selectActor(path:String) = system.actorSelection(path)
}
