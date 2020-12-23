package filepeer.core.transfer

import filepeer.core.ActorTestSuite
import akka.stream.scaladsl._
import akka.util.ByteString
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.{Await, Future}
import io.circe._
import io.circe.syntax._

class ProtocolSuite extends ActorTestSuite with LazyLogging {

  import ProtocolSuite._

  "ProtocolHandler's 'writeTextMessage'" should "write 1 text message from a flow" in {
    val payload = "this is a test msg"
    val size = payload.length

    val bsFut = Source.single(payload)
      .via(ProtocolHandlers.writeTextMessage)
      .runWith(bsSink)

    bsFut.map { bs =>
      val str = bs.utf8String
      logger.info("received:\n{}", str)

      str shouldBe (
        s"""Content-Length:$size
           |Message-Type:Text
           |--
           |$payload""".stripMargin
        )
    }
  }

  it should "write multiple text messages" in {
    val payload = "test msg"
    val size = payload.length

    val expectedMessage =
      s"""Content-Length:$size
         |Message-Type:Text
         |--
         |$payload""".stripMargin

    val expectedContent = expectedMessage * 10

    val bsFut = Source.repeat(payload)
      .take(10)
      .via(ProtocolHandlers.writeTextMessage)
      .runWith(bsSink)

    bsFut.map { bs =>
      val str = bs.utf8String
      logger.info("received:\n{}", str)

      str.shouldBe(expectedContent)
    }
  }

  "ProtocolHandler's 'writeBinaryMessage'" should "write a binary message" in {
    val user = DummyUser("Manuel", 22, List("programming", "skating", "skiing", "snowboarding"))
    val bytes = DummyUser.serialize(user)

    val byteString = ByteString(bytes)
    val bsFut = ProtocolHandlers.binaryMessageFromSource(Source.single(byteString), byteString.length)
      .runWith(bsSink)

    val bs = Await.result(bsFut, testTimeout)
    bs.size shouldBe >(bytes.length)
    bs.indexOfSlice(bytes) shouldBe >(-1)
  }

  it should "write a binary message with metaData" in {
    val user = DummyUser("Manuel", 22, List("programming", "skating", "skiing", "snowboarding"))
    val bytes = DummyUser.serialize(user)
    val json = user.asJson.noSpaces

    val headers = Seq(
      "File-Name" -> "dummy",
      "origin" -> "x-nico"
    )

    val expectedHeader =
      """File-Name:dummy
        |origin:x-nico""".stripMargin

    val byteString = ByteString(json)
    val bsFut = ProtocolHandlers.binaryMessageFromSource(Source.single(byteString), byteString.length, headers)
      .runWith(bsSink)

    bsFut.map { bs =>
      val str = bs.utf8String

      str should (include(expectedHeader) and include(json))
    }
  }

  "ProtocolHandler's 'reader'" should "read a message from a flow" in {
    val payload = "this is a test msg"
    val size = payload.length

    val msgFut = Source.single(payload)
      .via(ProtocolHandlers.writeTextMessage)
      .via(ProtocolHandlers.reader)
      .runWith(Sink.head)

    msgFut.map { msg =>
      val body = msg.body.utf8String
      logger.info("received headers: {}", msg.header)
      logger.info("received body:\n{}", body)

      body.shouldBe(payload)
    }
  }

  it should "read headers" in {
    val payload = "this is a test msg"
    val size = payload.length

    val msgFut = Source.single(payload)
      .via(ProtocolHandlers.writeTextMessage)
      .via(ProtocolHandlers.reader)
      .runWith(Sink.head)

    val expectedHeaders = Map(
      DefaultHeaders.CONTENT_LENGTH -> size.toString,
      DefaultHeaders.MESSAGE_TYPE -> DefaultHeaders.TEXT_MESSAGE_TYPE
    )

    msgFut.map { msg =>
      msg.header.shouldBe(expectedHeaders)
    }
  }

  it should "read multiple messages" in {
    val payload = "test msg"
    val size = payload.length

    val msgsFut = Source.repeat(payload)
      .take(10)
      .via(ProtocolHandlers.writeTextMessage)
      .via(ProtocolHandlers.reader)
      .runWith(Sink.seq)

    val msgs = Await.result(msgsFut, testTimeout)
    msgs should have size (10)

    forAll(msgs) { x =>
      x.body.utf8String shouldBe (payload)
    }
  }

  it should "serialize and deserialize binary data" in {
    val user = DummyUser("Manuel", 22, List("programming", "skating", "skiing", "snowboarding"))
    val bytes = DummyUser.serialize(user)

    val msgsFut = Source.repeat(user)
      .take(10)
      .map(DummyUser.serialize)
      .map(ByteString.apply)
      .flatMapConcat { bs =>
        ProtocolHandlers.binaryMessageFromSource(Source.single(bs), bs.length)
      }
      .via(ProtocolHandlers.reader)
      .runWith(Sink.seq)

    val msgs = Await.result(msgsFut, testTimeout)
    msgs should have size (10)

    forAll(msgs) { msg =>
      val obj = DummyUser.deserialize(msg.body.toArray)
      obj shouldBe (user)
    }
  }
}

object ProtocolSuite {
  val bsSink: Sink[ByteString, Future[ByteString]] = Sink.fold[ByteString, ByteString](ByteString.empty)(_ ++ _)
}
