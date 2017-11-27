package com.demos.chat

import akka.actor.{Actor, ActorRef, Props}
import com.demos.chat.ConnectionActor.Connected
import com.demos.chat.session.Session

/**
  *
  *
  * @author demos
  * @version 1.0
  */
class ConnectionActor(gateway: ActorRef) extends Actor {

  override def receive: Receive = notConnected()

  def notConnected(): Receive = {
    case Connected(webSocketActor) =>
      val session = context.actorOf(Session.props(self, gateway))
      context become connected(session, webSocketActor)
  }

  def connected(session: ActorRef, webSocketActor: ActorRef): Receive = {
    case HeartBeat() => println("Received heartbeat.")
    case chatRequest: ChatRequest => session ! chatRequest
    case chatResponse: ChatResponse => webSocketActor ! chatResponse
    case wrong => println(s"ConnectionActor: received wrong message [${wrong.toString}, ${sender().toString()}]")
  }
}

object ConnectionActor {
  def props(gateway: ActorRef) = Props(new ConnectionActor(gateway))
  case class Connected(webSocketActor: ActorRef)
}
