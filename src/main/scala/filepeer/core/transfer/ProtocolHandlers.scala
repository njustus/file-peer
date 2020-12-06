package filepeer.core.transfer

import akka.util.ByteString
import akka.stream.scaladsl._
import akka.NotUsed
import com.typesafe.scalalogging.LazyLogging

object ProtocolHandlers extends LazyLogging {
  sealed trait ReadState
  private case class Header(read: ByteString) extends ReadState {
    lazy val toMap: Map[String, String] = {
      val lines = read.utf8String.split('\n')
      val keyValuePairs = lines.flatMap { line =>
        val arr = line.split(":")
        if(arr.length == 2) {
          (arr(0), arr(1))::Nil
        } else {
          Nil
        }
      }

      keyValuePairs.toMap
    }

    def payloadSize: Option[Int] = toMap.get(DefaultHeaders.CONTENT_LENGTH).map(v => v.toInt)

    def ++(bs: ByteString): Header = this.copy(read++bs)

    override def toString(): String = s"Header(read: [${read.size}] bytes)"
  }

  private case class Body(header:Header, read: ByteString) extends ReadState {
    val bytesToRead: Int = header.payloadSize.get
    val remainingToRead: Int = bytesToRead - read.length
    val hasAllBytes: Boolean = remainingToRead <= 0

    def toMessage: ProtocolMessage = ProtocolMessage(header.toMap, read)

    def ++(bs:ByteString): Body = this.copy(read=read++bs)

    override def toString(): String = s"Body(header: $header, read: [${read.size}] bytes)"
  }

  case class ProtocolMessage(header: Map[String, String], body: ByteString) {
    override def toString(): String = s"ProtocolMessage(header: $header, body: [${body.size}] bytes)"
  }


  private val DELIMITER = ByteString("\n--\n")

  def reader: Flow[ByteString, ProtocolMessage, NotUsed] = Flow[ByteString].statefulMapConcat { () =>
    var state: ReadState  = Header(ByteString.empty)

    { currentBs =>
      state match {
        case h:Header =>
          val totalBs = h.read ++ currentBs
          val idx = totalBs.indexOfSlice(DELIMITER)
          if (idx > -1) {
            val (restHeader, body) = totalBs.splitAt(idx)
            val headerState = h ++ restHeader
            val bodyWithoutDelimiter = body.drop(DELIMITER.size)
            val bodyState = Body(headerState, bodyWithoutDelimiter)

            logger.debug(s"header read with delimiter: $headerState")
            if(bodyState.hasAllBytes) {
              state = Header(ByteString.empty)
              bodyState.toMessage::Nil
            }
            else {
              state = bodyState
              Nil
            }
          } else {
            logger.debug(s"header read: $h bs size: ${currentBs.length}")
            state = h ++ currentBs
            Nil
          }
        case b:Body =>
          val (needed, remaining) = currentBs.splitAt(b.remainingToRead)
          val bodyState = b ++ needed

          logger.debug(s"body read: $bodyState remaining bytes: ${remaining.length}")

          if(bodyState.hasAllBytes) {
            state = Header(ByteString.empty)
            bodyState.toMessage::Nil
          }
          else {
            state = bodyState
            Nil
          }
      }
    }
  }

  def writeTextMessage: Flow[String, ByteString, NotUsed] = Flow[String].mapConcat { str =>
    val size = str.length
    val headers = DefaultHeaders.headers(DefaultHeaders.contentLength(size), DefaultHeaders.textMessage)

    logger.debug(s"serializing TextMessage with [$size] bytes")
    List(headers, DELIMITER, ByteString(str))
  }

}


object DefaultHeaders {
  val CONTENT_LENGTH = "Content-Length"
  val MESSAGE_TYPE = "Message-Type"

  val TEXT_MESSAGE_TYPE = "Text"
  val BINARY_MESSAGE_TYPE = "Binary"

  def makeHeader(name: String, value: String): String = name + ":" + value

  def textMessage: String = makeHeader(MESSAGE_TYPE, TEXT_MESSAGE_TYPE)
  def binaryMessage: String = makeHeader(MESSAGE_TYPE, BINARY_MESSAGE_TYPE)
  def contentLength(size:Int): String = makeHeader(CONTENT_LENGTH, size.toString)

  def headers(headers: String*): ByteString = ByteString(headers.mkString("\n"))
}
