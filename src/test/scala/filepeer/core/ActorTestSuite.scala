package filepeer.core

import akka.actor.ActorSystem
import akka.stream.Materializer
import org.scalatest._
import matchers._
import flatspec._

abstract class ActorTestSuite extends AsyncFlatSpec with should.Matchers with Inspectors with BeforeAndAfterAll {
  implicit val system = ActorSystem("filepeer-testsystem")
  implicit val mat = Materializer.matFromSystem
  implicit val exec = system.dispatcher

  override def afterAll() = {
    system.terminate()
  }
}
