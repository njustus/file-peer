package filepeer.core.transfer

import io.circe.generic.JsonCodec
import org.apache.commons.lang3.SerializationUtils

@JsonCodec
@SerialVersionUID(38L)
case class DummyUser(name: String, age: Int, tags: List[String])

object DummyUser {

  val personInResourceFile = DummyUser("tom", 19, List("hockey", "tennis", "baseball"))

  val serialize: DummyUser => Array[Byte] = SerializationUtils.serialize
  val deserialize: Array[Byte] => DummyUser = SerializationUtils.deserialize[DummyUser]
}
