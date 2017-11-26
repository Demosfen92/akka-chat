package com.demos.chat

import akka.actor.{Actor, ActorRef, Props}
import com.demos.chat.ConnectionActor.Connected
import com.demos.chat.session.Session.InitializeSession

/**
  *
  *
  * @author demos
  * @version 1.0
  */
class ConnectionActor(session: ActorRef) extends Actor {

  override def preStart(): Unit = session ! InitializeSession(self)

  override def receive: Receive = notConnected()

  def notConnected(): Receive = {
    case Connected(webSocketActor) => context become connected(webSocketActor)
  }

  def connected(webSocketActor: ActorRef): Receive = {
    case HeartBeat() => println("Received heartbeat.")
    case chatRequest: ChatRequest => session ! chatRequest
    case chatResponse: ChatResponse => webSocketActor ! chatResponse
    case wrong => println(s"ConnectionActor: received wrong message [${wrong.toString}, ${sender().toString()}]")
  }
}

object ConnectionActor {
  def props(session: ActorRef) = Props(new ConnectionActor(session))
  case class Connected(webSocketActor: ActorRef)
}
