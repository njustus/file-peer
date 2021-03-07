package filepeer.core.transfer

import filepeer.core.TransferEnv
import io.circe.generic.JsonCodec

object TransferServer {
  @JsonCodec
  sealed trait TransferMsg
  case class TransferPreview(fileNames: List[String]) extends TransferMsg
  case class FileTransfer(fileName: String, content: Array[Byte]) extends TransferMsg

  val FILENAME_HEADER = "File-Name"

  def createTargetDir(conf: TransferEnv): Unit = {
    better.files.File(conf.targetDir).createDirectories()
  }
}
