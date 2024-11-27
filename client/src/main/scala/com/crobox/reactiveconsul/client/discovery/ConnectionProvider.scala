package com.crobox.reactiveconsul.client.discovery

import org.apache.pekko.actor.ActorRef

import scala.concurrent.{ExecutionContext, Future}

trait ConnectionProvider {
  def getConnection: Future[Any]
  def returnConnection(connectionHolder: ConnectionHolder): Unit = ()
  def destroy(): Unit = ()
  def getConnectionHolder(identifier: String, loadBalancerRef: ActorRef)(implicit ec: ExecutionContext): Future[ConnectionHolder] = getConnection.map { connection =>
    new ConnectionHolder {
      override def connection: Future[Any] = getConnection
      override val loadBalancer: ActorRef = loadBalancerRef
      override val id: String = identifier
    }
  }
}
