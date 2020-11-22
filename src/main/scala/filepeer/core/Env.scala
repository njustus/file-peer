package filepeer.core

case class Env(
  discovery: DiscoveryEnv,
  transfer: TransferEnv
)

case class DiscoveryEnv(
  address: Address,
  includeLocalhost: Boolean
)

case class TransferEnv(
  address: Address
)

case class Address(
  host: String,
  port: Int
)
