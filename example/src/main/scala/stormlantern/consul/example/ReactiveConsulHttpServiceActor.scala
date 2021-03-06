package stormlantern.consul.example

import akka.actor.{ Actor, Props }
import spray.routing.HttpService

import scala.concurrent.ExecutionContext

class ReactiveConsulHttpServiceActor extends Actor with ReactiveConsulHttpService {

  def actorRefFactory = context

  def receive = runRoute(reactiveConsulRoute)
}

object ReactiveConsulHttpServiceActor {
  def props() = Props(classOf[ReactiveConsulHttpServiceActor])
}

trait ReactiveConsulHttpService extends HttpService {
  implicit def executionContext: ExecutionContext = actorRefFactory.dispatcher

  val reactiveConsulRoute =
    pathPrefix("api") {
      path("identify") {
        get {
          complete(s"Hi, I'm a ${System.getenv("SERVICE_NAME")} called ${System.getenv("INSTANCE_NAME")}")
        }
      } ~
        path("talk") {
          get {
            complete("pong")
          }
        }
    }
}