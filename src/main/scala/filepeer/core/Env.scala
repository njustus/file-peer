package filepeer.core

import java.net.InetSocketAddress
import java.nio.file.Path

case class Env(
  discovery: DiscoveryEnv,
  transfer: TransferEnv,
  features: Set[Features.Feature]
) {
  def downloadDir: Path = transfer.targetDir

  def discoveryFeatureIsEnabled:Boolean = features.contains(Features.Discovery)
}

case class DiscoveryEnv(
  address: Address,
  includeLocalhost: Boolean
) {
  def listenerAddress: InetSocketAddress = new InetSocketAddress(address.host, address.port)
  def broadcastAddress: InetSocketAddress = new InetSocketAddress("255.255.255.255", address.port)
}

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

object Features extends Enumeration {
  type Feature = Value
  val Discovery = Value
}
