package filepeer.core.transfer

import filepeer.core.ActorTestSuite
import akka.stream.scaladsl._
import akka.util.ByteString
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.Await
import org.apache.commons.lang3.SerializationUtils
import org.scalatest._
import java.nio.file._

import cats.data.NonEmptyList
import filepeer.core.Env
import org.scalatest.concurrent._

import scala.concurrent._
import scala.concurrent.duration._

class ClientSuite extends ActorTestSuite with Eventually with LazyLogging {

  val tempDir = Files.createTempDirectory("filepeer")
  val sourceFile = better.files.File.currentWorkingDirectory / "src" / "test" / "resources" / "dummy-user"
  implicit val env2:Env = env.copy(transfer=env.transfer.copy(targetDir=tempDir))
  val localhost = env2.transfer.address

  val fileWrittenPromise = Promise[FileReceiver.FileSaved]()
  lazy val receiver = new FileReceiver(new FileReceiver.FileSavedObserver() {
    override def fileSaved(fs:FileReceiver.FileSaved):Unit = fileWrittenPromise.success(fs)
  })

  val transfer = new TransferServer(receiver)
  val sender = new Client()

  override def afterAll(): Unit = {
    super.afterAll()
    better.files.File(tempDir).delete(swallowIOExceptions = true)
  }

    "Client/FileSender" should "read a file into memory" in {
    sourceFile.path.toFile should exist

    val bsFut = Client.sourceFromPath(sourceFile.path).runWith(ProtocolSuite.bsSink)
    val bs = Await.result(bsFut, testTimeout)

    val expectedBytes = DummyUser.serialize(DummyUser.personInResourceFile)
    val (start, end) = bs.splitAt(bs.length-expectedBytes.length)

    end shouldBe (expectedBytes)
    start.utf8String shouldBe (s"""Content-Length:${expectedBytes.length}
         |Message-Type:Binary
         |File-Name:${sourceFile.name}
         |--\n""".stripMargin)
  }

  it should "send a file into a stream" in {
    sourceFile.path.toFile should exist

    val msgFut = Client.sourceFromPath(sourceFile.path)
      .via(ProtocolHandlers.reader)
      .runWith(Sink.head)
    val msg = Await.result(msgFut, testTimeout)

    msg.header(TransferServer.FILENAME_HEADER) shouldBe (sourceFile.name)
    msg.header(DefaultHeaders.CONTENT_LENGTH).toInt shouldBe (sourceFile.size)
    msg.body.length shouldBe (sourceFile.size)
  }

  it should "send a file over wire" in {
    sourceFile.path.toFile should exist

    val expectedFile = tempDir.resolve(sourceFile.name).toFile
    val fut = sender.sendFile(localhost, NonEmptyList.of(sourceFile.path))
    Await.ready(fut, testTimeout)
    Await.ready(fileWrittenPromise.future, testTimeout)

    expectedFile should exist
    val bytes = better.files.File(expectedFile.toPath).byteArray
    val fileUser = DummyUser.deserialize(bytes)
    fileUser shouldBe (DummyUser.personInResourceFile)
  }
}
