package filepeer.core

import akka.actor.ActorSystem
import akka.stream.Materializer
import org.scalatest._
import matchers._
import flatspec._
import scala.concurrent.duration._

abstract class ActorTestSuite extends AsyncFlatSpec with should.Matchers with Inspectors with BeforeAndAfterAll {
  implicit val system = ActorSystem("filepeer-testsystem")
  implicit val mat = Materializer.matFromSystem
  implicit val exec = system.dispatcher

  implicit val testTimeout = 20 seconds
  override def afterAll() = {
    system.terminate()
  }
}
