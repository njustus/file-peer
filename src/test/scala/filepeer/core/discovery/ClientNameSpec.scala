package filepeer.core.discovery

import filepeer.FilePeerTestSuite
import filepeer.core._
import scala.collection.mutable

class ClientNameSpec extends FilePeerTestSuite {
  import DiscoveryService.ClientName

    val cl = ClientName("localhost", "127.0.0.1", 5050)

  "The ClientName dataclass" should "create Address() based on ip & port" in {
    cl.address shouldBe (Address("127.0.0.1", 5050))
  }

  it should "implement reflexive equals" in {
    (cl == cl) shouldBe (true)
  }

  it should "implement equals based on ip" in {
    val cl2 = cl.copy(hostName="nico")
    (cl == cl2) shouldBe (true)

    val cl3 = cl.copy(hostName="nico", port=7001)
    (cl == cl3) shouldBe (true)
  }

  it should "be useable inside a LinkedHashSet" in {
    val hashSet = mutable.LinkedHashSet(cl)
    hashSet += cl
    hashSet.size shouldBe (1)

    hashSet += cl.copy()
    hashSet.size shouldBe (1)

    hashSet += ClientName("nico", "192.168.0.1", 7080)
    hashSet.size shouldBe (2)
  }
}
