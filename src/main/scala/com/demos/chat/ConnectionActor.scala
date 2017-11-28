package com.demos.chat

import akka.actor.{Actor, ActorRef, PoisonPill, Props}
import com.demos.chat.ConnectionActor.{Connected, ConnectionClosed}
import com.demos.chat.messages.{ChatRequest, ChatResponse, HeartBeat}
import com.demos.chat.session.Session

/**
  * Intermediate actor created for each client in order
  * to receive incoming messages and send back outgoing messages.
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
    case ConnectionClosed =>
      webSocketActor ! PoisonPill
      context stop self
    case unexpected => println(s"ConnectionActor: received unexpected message [${unexpected.toString}, ${sender().toString()}]")
  }
}

object ConnectionActor {
  def props(gateway: ActorRef) = Props(new ConnectionActor(gateway))
  case class Connected(webSocketActor: ActorRef)
  case object ConnectionClosed
}
