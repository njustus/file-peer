package filepeer.core

import java.net.InetSocketAddress
import java.nio.file.Path

import scala.concurrent.duration.FiniteDuration

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
  broadcast: Address,
  includeLocalhost: Boolean,
  broadcastInterval: FiniteDuration
) {
  def listenerAddress: InetSocketAddress = address.inetSocketAddress
  def broadcastAddress: InetSocketAddress = broadcast.inetSocketAddress
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
  def inetSocketAddress: InetSocketAddress = new InetSocketAddress(host, port)
}

object Features extends Enumeration {
  type Feature = Value
  val Discovery = Value
}
