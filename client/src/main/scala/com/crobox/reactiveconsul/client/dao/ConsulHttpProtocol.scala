package com.crobox.reactiveconsul.client.dao

import java.util.UUID

import spray.json._

import scala.util.control.NonFatal
import java.util.Base64

trait ConsulHttpProtocol extends DefaultJsonProtocol {

  implicit val uuidFormat: JsonFormat[UUID] = new JsonFormat[UUID] {
    override def read(json: JsValue): UUID = json match {
      case JsString(uuid) => try {
        UUID.fromString(uuid)
      } catch {
        case NonFatal(e) => deserializationError("Expected UUID, but got " + uuid)
      }
      case x => deserializationError("Expected UUID as JsString, but got " + x)
    }

    override def write(obj: UUID): JsValue = JsString(obj.toString)
  }

  implicit val binaryDataFormat: JsonFormat[BinaryData] = new JsonFormat[BinaryData] {
    override def read(json: JsValue): BinaryData = json match {
      case JsString(data) => try {
        BinaryData(Base64.getMimeDecoder.decode(data))
      } catch {
        case NonFatal(e) => deserializationError("Expected base64 encoded binary data, but got " + data)
      }
      case x => deserializationError("Expected base64 encoded binary data as JsString, but got " + x)
    }

    override def write(obj: BinaryData): JsValue = JsString(Base64.getMimeEncoder.encodeToString(obj.data))
  }

  implicit val serviceFormat: RootJsonFormat[ServiceInstance] = jsonFormat(
    (node: String, address: String, serviceId: String, serviceName: String, serviceTags: Option[Set[String]], serviceAddress: String, servicePort: Int) =>
      ServiceInstance(node, address, serviceId, serviceName, serviceTags.getOrElse(Set.empty), serviceAddress, servicePort),
    "Node", "Address", "ServiceID", "ServiceName", "ServiceTags", "ServiceAddress", "ServicePort"
  )

  implicit val nodeFormat: RootJsonFormat[Node] = jsonFormat(Node.apply, "Node", "Address")
  implicit val healthServiceFormat: RootJsonFormat[Service] = jsonFormat(Service.apply, "ID", "Service", "Tags", "Address", "Port")
  implicit val healthServiceInstanceFormat: RootJsonFormat[HealthServiceInstance] = jsonFormat(HealthServiceInstance.apply, "Node", "Service")

  implicit val httpCheckFormat: RootJsonFormat[HttpHealthCheck] = jsonFormat(HttpHealthCheck.apply, "HTTP", "Interval")
  implicit val scriptCheckFormat: RootJsonFormat[ScriptHealthCheck] = jsonFormat(ScriptHealthCheck.apply, "Script", "Interval")
  implicit val ttlCheckFormat: RootJsonFormat[TTLHealthCheck] = jsonFormat(TTLHealthCheck.apply, "TTL")
  implicit val checkWriter: JsonFormat[HealthCheck] = lift {
    new JsonWriter[HealthCheck] {
      override def write(obj: HealthCheck): JsValue = obj match {
        case obj: ScriptHealthCheck => obj.toJson
        case obj: HttpHealthCheck   => obj.toJson
        case obj: TTLHealthCheck    => obj.toJson
      }
    }
  }
  implicit val serviceRegistrationFormat: RootJsonFormat[ServiceRegistration] = jsonFormat(ServiceRegistration.apply, "Name", "ID", "Tags", "Address", "Port", "Check")
  implicit val sessionCreationFormat: RootJsonFormat[SessionCreation] = jsonFormat(SessionCreation.apply, "LockDelay", "Name", "Node", "Checks", "Behavior", "TTL")
  implicit val keyDataFormat: RootJsonFormat[KeyData] = jsonFormat(KeyData.apply, "Key", "CreateIndex", "ModifyIndex", "LockIndex", "Flags", "Value", "Session")
  implicit val sessionInfoFormat: RootJsonFormat[SessionInfo] = jsonFormat(SessionInfo.apply, "LockDelay", "Checks", "Node", "ID", "CreateIndex", "Name", "Behavior", "TTL")
}
