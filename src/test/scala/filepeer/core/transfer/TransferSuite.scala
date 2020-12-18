package filepeer.core.transfer

import filepeer.core.ActorTestSuite
import akka.stream.scaladsl._
import akka.util.ByteString
import com.typesafe.scalalogging.LazyLogging
import scala.concurrent.Await
import org.apache.commons.lang3.SerializationUtils
import org.scalatest._
import java.nio.file._
import filepeer.core.Env
import org.scalatest.concurrent._
import scala.concurrent.duration._

class TransferSuite extends ActorTestSuite with Eventually with LazyLogging {

  val tempDir = Files.createTempDirectory("filepeer")
  val sourceFile = better.files.File.currentWorkingDirectory / "src" / "test" / "resources" / "dummy-user"
  implicit val env2:Env = env.copy(transfer=env.transfer.copy(targetDir=tempDir))
  lazy val transferSvc = new TransferService()

  override def afterAll(): Unit = {
    better.files.File(tempDir).delete(true)
  }

  "TransferService" should "read a file into memory" in {
    sourceFile.path.toFile should exist

    val contentFut = transferSvc.fileSource(sourceFile.path).runWith(Sink.head)
    val TransferService.FileTransfer(name, content) = Await.result(contentFut, testTimeout)

    name shouldBe ("dummy-user")
    val user = DummyUser.deserialize(content)
    user shouldBe (DummyUser.personInResourceFile)
  }

  it should "write a file with filename header" in {
    sourceFile.path.toFile should exist

    val msgFut = transferSvc.serializedFileSource(sourceFile.path).via(ProtocolHandlers.reader).runWith(Sink.head)

    msgFut.map { msg =>
      msg.header(TransferService.FILENAME_HEADER) shouldBe ("dummy-user")
    }
  }

  it should "write bytes into a file" in {
    val bytes = DummyUser.serialize(DummyUser.personInResourceFile)
    val msg = TransferService.FileTransfer(s"dummy-generated-user", bytes)

    Source.single(msg).runWith(transferSvc.fileSink)

    Thread.sleep(5000)
    val path = tempDir.resolve("dummy-generated-user")
    path.toFile should exist
    val userInFile = DummyUser.deserialize(better.files.File(path).byteArray)
    userInFile shouldBe (DummyUser.personInResourceFile)
  }
}
