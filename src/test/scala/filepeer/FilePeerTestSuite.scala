package filepeer

import filepeer.core.Env
import org.scalatest.flatspec.AsyncFlatSpecLike
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, Inspectors}
import org.scalatest.matchers.should
import pureconfig._
import pureconfig.generic.auto._

trait FilePeerTestSuite
  extends AsyncFlatSpecLike
    with should.Matchers
    with Inspectors
    with BeforeAndAfterAll
    with BeforeAndAfter {
  implicit val env: Env = {
    ConfigSource.resources("test-application.conf")
      .withFallback(ConfigSource.default)
      .at("file-peer")
      .loadOrThrow[Env]
  }
}
