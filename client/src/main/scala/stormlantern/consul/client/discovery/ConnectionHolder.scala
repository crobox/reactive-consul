package stormlantern.consul.client.discovery

import akka.actor.ActorRef

import scala.concurrent.Future

trait ConnectionHolder {
  val key: String
  def loadBalancer: ActorRef
  def connection: Future[Any]
}
