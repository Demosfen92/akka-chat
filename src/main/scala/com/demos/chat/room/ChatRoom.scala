package com.demos.chat.room

import akka.actor.{Actor, ActorRef, Props}
import com.demos.chat.room.ChatRoom.JoinRoomResults.{AlreadyJoined, SuccessfullyJoined}
import com.demos.chat.room.ChatRoom.{JoinRoom, ReceivedMessage, SendMessageToAll}

/**
  *
  *
  * @author demos
  * @version 1.0
  */
class ChatRoom extends Actor {

  override def receive: Receive = chatRoom(Map.empty)

  def chatRoom(members: Map[String, ActorRef]): Receive = {
    case JoinRoom(username) => members.get(username) match {
      case None =>
        context become chatRoom(members + (username -> sender()))
        sender() ! SuccessfullyJoined
      case Some(_) => sender() ! AlreadyJoined
    }
    case SendMessageToAll(author, message) =>
      members foreach {
        case (_, recipient) => recipient ! ReceivedMessage(author, message)
      }
  }
}

object ChatRoom {
  def props() = Props(new ChatRoom())
  case class JoinRoom(username: String)
  case class SendMessageToAll(author: String, message: String)
  case class ReceivedMessage(author: String, message: String)

  object JoinRoomResults {
    sealed trait JoinRoomResult
    case object SuccessfullyJoined extends JoinRoomResult
    case object AlreadyJoined extends JoinRoomResult
  }
}
