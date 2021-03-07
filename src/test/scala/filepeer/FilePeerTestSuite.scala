package filepeer

import filepeer.core.Env
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, Inspectors}
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should
import pureconfig._
import pureconfig.generic.auto._

trait FilePeerTestSuite
  extends AsyncFlatSpec
    with should.Matchers
    with Inspectors
    with BeforeAndAfterAll
    with BeforeAndAfter {
  implicit val env: Env = ConfigSource.default.at("file-peer").loadOrThrow[Env]
}
