package com.demos.chat

import akka.actor.{Actor, ActorRef, PoisonPill, Props, Terminated}
import com.demos.chat.ConnectionActor.Connected

/**
  *
  *
  * @author demos
  * @version 1.0
  */
class ConnectionActor extends Actor {

  override def receive: Receive = notConnected()

  def notConnected(): Receive = {
    case Connected(webSocketActor) => context become connected(webSocketActor)
  }

  def connected(webSocketActor: ActorRef): Receive = {
    case chatRequest: ChatRequest => chatRequest match {
      case HeartBeat => println("Received heartbeat")
      case SimpleMessage(message) => webSocketActor ! ResponseMessage(message)
      case _ => webSocketActor ! Error("Unexpected message")
    }
    case chatResponse: ChatResponse => webSocketActor ! chatResponse
  }
}

object ConnectionActor {
  def props() = Props(new ConnectionActor())
  case class Connected(webSocketActor: ActorRef)
}
