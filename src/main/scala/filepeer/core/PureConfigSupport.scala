package filepeer.core

import pureconfig.ConfigReader

trait PureConfigSupport {
  implicit val featureReader: ConfigReader[Features.Feature] = {
    ConfigReader[String].map(Features.withName)
  }
}
