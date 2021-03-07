package filepeer

import filepeer.core.{Env, PureConfigSupport}
import org.scalatest.flatspec.AsyncFlatSpecLike
import org.scalatest.matchers.should
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, Inspectors}
import pureconfig.ConfigSource
import pureconfig.generic.auto._

trait FilePeerTestSuite
  extends AsyncFlatSpecLike
    with should.Matchers
    with Inspectors
    with BeforeAndAfterAll
    with BeforeAndAfter
    with PureConfigSupport {

  implicit val env: Env = {
    ConfigSource.resources("test-application.conf")
      .withFallback(ConfigSource.default)
      .at("file-peer")
      .loadOrThrow[Env]
  }
}
