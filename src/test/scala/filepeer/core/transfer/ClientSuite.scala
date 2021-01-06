package filepeer.core.transfer

import java.nio.charset.StandardCharsets

import filepeer.core.ActorTestSuite
import akka.stream.scaladsl._
import akka.util.ByteString
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.Await
import java.nio.file._

import cats.data.NonEmptyList
import filepeer.core.Env
import org.scalatest.concurrent._
import rx.lang.scala.Subject

import scala.concurrent._
import scala.concurrent.duration._
import scala.util.Using

class ClientSuite extends ActorTestSuite with Eventually with LazyLogging {

  val tempDir = Files.createTempDirectory("filepeer")
  val sourceFile = better.files.File.currentWorkingDirectory / "src" / "test" / "resources" / "dummy-user"
  implicit val env2: Env = env.copy(transfer = env.transfer.copy(targetDir = tempDir))
  val localhost = env2.transfer.address

  val DENIED_FILENAME = "denied-dummy-user.txt"

  private val fileWritten$ = Subject[FileReceiver.FileSaved]()
  lazy val receiver = new FileReceiver(new FileReceiver.FileSavedObserver() {
    override def accept(file: FileReceiver.FileSaved): Boolean = file.name != DENIED_FILENAME

    override def fileSaved(fs: FileReceiver.FileSaved): Unit = fileWritten$.onNext(fs)
  })

  val transfer = new TransferServer(receiver)
  val sender = new Client()

  override def afterAll(): Unit = {
    super.afterAll()
    better.files.File(tempDir).delete(swallowIOExceptions = true)
    fileWritten$.onCompleted()
  }

  "Client/FileSender" should "read a file into memory" in {
    sourceFile.path.toFile should exist

    val bsFut = Client.sourceFromPath(sourceFile.path).runWith(ProtocolSuite.bsSink)
    val bs = Await.result(bsFut, testTimeout)

    val expectedBytes = DummyUser.serialize(DummyUser.personInResourceFile)
    val (start, end) = bs.splitAt(bs.length - expectedBytes.length)

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

    val fileWrittenPromise = Promise[FileReceiver.FileSaved]()
    fileWritten$
      .filter(_.name === sourceFile.name)
      .first
      .subscribe(x => fileWrittenPromise.success(x))

    val expectedFile = tempDir.resolve(sourceFile.name).toFile
    val fut = sender.sendFile(localhost, NonEmptyList.of(sourceFile.path))
    Await.ready(fut, testTimeout)
    Await.ready(fileWrittenPromise.future, testTimeout)

    expectedFile should exist
    val bytes = better.files.File(expectedFile.toPath).byteArray
    val fileUser = DummyUser.deserialize(bytes)
    fileUser shouldBe (DummyUser.personInResourceFile)
  }

  "The receiver" should "read from tcp till file end reached, even if the file is big" in {
    val source = better.files.File.newTemporaryFile("big")
    val requiredBytes = 20 * 1024 * 1024 //20MB
    val charset = StandardCharsets.UTF_8
    val line = "this is a dummy line."
    val byteSize = line.getBytes(charset).length

    Using.resource(source) { _ =>
      val writingSourceFile = Source.repeat(line)
        .take(Math.ceil(requiredBytes.toDouble / byteSize.toDouble).toInt)
        .map(x => ByteString.apply(x, charset))
        .runWith(FileIO.toPath(source.path))

      Await.ready(writingSourceFile, testTimeout)
      require(source.size >= requiredBytes, s"the generated source file must have a size >= $requiredBytes")

      val fileWrittenPromise = Promise[FileReceiver.FileSaved]()
      fileWritten$
        .filter(_.name === source.name)
        .first
        .subscribe(x => fileWrittenPromise.success(x))

      val fut = sender.sendFile(localhost, NonEmptyList.of(source.path))
      Await.ready(fut, testTimeout)
      val fileWritten = Await.result(fileWrittenPromise.future, testTimeout)

      fileWritten.path.toFile should exist
      fileWritten.name shouldBe (source.name)
      Files.size(fileWritten.path) shouldBe (source.size)
    }(rsc => rsc.delete())
  }

  it should "deny files that aren't accepted by the provided FileObserver" in {
    val source = better.files.File(tempDir) / DENIED_FILENAME
    source.writeText("a test text")

    val fileWrittenPromise = Promise[FileReceiver.FileSaved]()
    fileWritten$
      .filter(_.name === source.name)
      .subscribe(x => fileWrittenPromise.success(x))

    val fut = sender.sendFile(localhost, NonEmptyList.of(source.path))
    Await.ready(fut, testTimeout)
    an [TimeoutException] shouldBe thrownBy(Await.result(fileWrittenPromise.future, testTimeout))
  }
}
