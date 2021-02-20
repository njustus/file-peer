package filepeer.core

import java.nio.file.Path

case class Env(
  discovery: DiscoveryEnv,
  transfer: TransferEnv
) {
  def downloadDir: Path = transfer.targetDir
}

case class DiscoveryEnv(
  address: Address,
  includeLocalhost: Boolean
)

case class TransferEnv(
  address: Address,
  targetDir: Path
)

case class Address(
  host: String,
  port: Int
) {
  def uriString: String = s"http://$host:$port/"
  def format: String = s"$host:$port"
}

