package stormlantern.consul.client.loadbalancers

import akka.actor.Status.Failure
import akka.actor.{ Props, Actor, ActorLogging }
import LoadBalancerActor._
import stormlantern.consul.client.discovery.{ ConnectionProvider, ConnectionHolder }
import stormlantern.consul.client.ServiceUnavailableException
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable

class LoadBalancerActor(loadBalancer: LoadBalancer, key: String) extends Actor with ActorLogging {

  import akka.pattern.pipe

  // Actor state
  val connectionProviders = mutable.Map.empty[String, ConnectionProvider]

  override def postStop(): Unit = {
    log.debug(s"LoadBalancerActor for $key stopped, destroying all connection providers")
    connectionProviders.values.foreach(_.destroy())
  }

  def receive: PartialFunction[Any, Unit] = {

    case GetConnection ⇒
      selectConnection match {
        case Some((id, connectionProvider)) ⇒ connectionProvider.getConnectionHolder(id, self) pipeTo sender
        case None                           ⇒ sender ! Failure(ServiceUnavailableException(key))
      }
    case ReturnConnection(connection)        ⇒ returnConnection(connection)
    case AddConnectionProvider(id, provider) ⇒ addConnectionProvider(id, provider)
    case RemoveConnectionProvider(id)        ⇒ removeConnectionProvider(id)
    case HasAvailableConnectionProvider      ⇒ sender ! connectionProviders.nonEmpty
  }

  def selectConnection: Option[(String, ConnectionProvider)] =
    loadBalancer.selectConnection.flatMap(id ⇒ connectionProviders.get(id).map(id -> _))

  def returnConnection(connection: ConnectionHolder): Unit = {
    connectionProviders.get(connection.id).foreach(_.returnConnection(connection))
    loadBalancer.connectionReturned(connection.id)
  }

  def addConnectionProvider(id: String, provider: ConnectionProvider): Unit = {
    connectionProviders.put(id, provider)
    loadBalancer.connectionProviderAdded(id)
  }

  def removeConnectionProvider(id: String): Unit = {
    connectionProviders.remove(id).foreach(_.destroy())
    loadBalancer.connectionProviderRemoved(id)
  }
}

object LoadBalancerActor {
  // Props
  def props(loadBalancer: LoadBalancer, key: String) = Props(new LoadBalancerActor(loadBalancer, key))
  // Messsages
  case object GetConnection
  case class ReturnConnection(connection: ConnectionHolder)
  case class AddConnectionProvider(id: String, provider: ConnectionProvider)
  case class RemoveConnectionProvider(id: String)
  case object HasAvailableConnectionProvider
}
