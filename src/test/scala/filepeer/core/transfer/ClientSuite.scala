package filepeer.core.transfer

import java.nio.file._

import cats.data.NonEmptyList
import com.typesafe.scalalogging.LazyLogging
import filepeer.core.{ActorTestSuite, Env}
import org.scalatest.concurrent._
import rx.lang.scala.Subject

import scala.concurrent.{Await, _}
import scala.concurrent.duration._

class ClientSuite extends ActorTestSuite with Eventually with LazyLogging {

  private val tempDir = Files.createTempDirectory("filepeer")
  private val sourceFile = better.files.File.currentWorkingDirectory / "src" / "test" / "resources" / "dummy-user"
  private implicit val env2: Env = env.copy(transfer = env.transfer.copy(targetDir = tempDir))
  private val localhost = env2.transfer.address

  val DENIED_FILENAME = "denied-dummy-user.txt"

  private val fileWritten$ = Subject[FileReceiver.FileSaved]()
  lazy val receiver = new FileReceiver(new FileReceiver.FileSavedObserver() {
    override def accept(file: FileReceiver.FileSaved): Future[Boolean] = Future.successful(file.name != DENIED_FILENAME)

    override def fileSaved(fs: FileReceiver.FileSaved): Unit = fileWritten$.onNext(fs)
  })

  val transfer = new HttpReceiver(receiver)
  val sender = new HttpClient()

  override def beforeAll(): Unit = {
    super.beforeAll()
    Await.ready(transfer.bind(localhost), 5 seconds)
  }

  override def afterAll(): Unit = {
    super.afterAll()
    better.files.File(tempDir).delete(swallowIOExceptions = true)
    fileWritten$.onCompleted()
  }

  "Client/FileSender" should "send a file over wire" in {
    sourceFile.path.toFile should exist

    val fileWrittenPromise = Promise[FileReceiver.FileSaved]()
    fileWritten$
      .filter(_.name === sourceFile.name)
      .first
      .subscribe(x => fileWrittenPromise.success(x))

    val expectedFile = tempDir.resolve(sourceFile.name).toFile
    val fut = sender.sendFile(localhost, NonEmptyList.of(sourceFile.path))
    val uploadResult = Await.result(fut, testTimeout)
    Await.ready(fileWrittenPromise.future, testTimeout)

    uploadResult shouldBe (Client.Done)
    expectedFile should exist
    val bytes = better.files.File(expectedFile.toPath).byteArray
    val fileUser = DummyUser.deserialize(bytes)
    fileUser shouldBe (DummyUser.personInResourceFile)
  }

  it should "return 'Rejected' if server rejected the upload" in {
    val source = better.files.File(tempDir) / DENIED_FILENAME
    source.writeText("a test text")

    val fut = sender.sendFile(localhost, NonEmptyList.of(source.path))
    val uploadResult = Await.result(fut, testTimeout)
    uploadResult shouldBe a [Client.Rejected]

    val rejectedResult = uploadResult.asInstanceOf[Client.Rejected]
    rejectedResult.sourceFile shouldBe (source.path)
    rejectedResult.server shouldBe (localhost)
  }
}
