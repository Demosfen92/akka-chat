package com.demos.chat.room

import akka.actor.{Actor, ActorRef, Props, Terminated}
import com.demos.chat.room.ChatRoom.JoinRoomResults.{AlreadyJoined, SuccessfullyJoined}
import com.demos.chat.room.ChatRoom._

/**
  * Chat room actor. Stores joined clients.
  *
  * @author demos
  * @version 1.0
  */
class ChatRoom extends Actor {

  override def receive: Receive = chatRoom(Map.empty, Map.empty)

  def chatRoom(sessionsToName: Map[ActorRef, String], nameToSession: Map[String, ActorRef]): Receive = {
    case JoinRoom(username) => sessionsToName.get(sender()) match {
      case None =>
        val updatedMembers = sessionsToName + (sender() -> username)
        sender() ! SuccessfullyJoined(updatedMembers.values)
        sessionsToName foreach {
          case (recipient, _) => recipient ! MemberJoined(username)
        }
        context watch sender()
        context become chatRoom(updatedMembers, nameToSession + (username -> sender()))
      case Some(_) => sender() ! AlreadyJoined(username)
    }
    case SendMessageToAll(author, message) =>
      sessionsToName foreach {
        case (recipient, _) => recipient ! ReceivedMessage(author, message)
      }
    case SendDirectMessage(author, recipientName, message) =>
      nameToSession.get(recipientName) match {
        case Some(recipient) =>
          recipient ! ReceivedDirectMessage(author, message)
          sender() ! MessageSent
        case None => sender() ! NoSuchMembers(recipientName)
      }
    case Terminated(_) =>
      val username = sessionsToName(sender())
      val updatedMembers = sessionsToName - sender()
      context become chatRoom(updatedMembers, nameToSession - username)
      updatedMembers foreach {
        case (recipient, _) => recipient ! MemberDisconnected(username)
      }
  }
}

object ChatRoom {

  def props() = Props(new ChatRoom())

  case class JoinRoom(username: String)
  case class MemberJoined(username: String)
  case class MemberDisconnected(username: String)
  case class SendMessageToAll(author: String, message: String)
  case class SendDirectMessage(author: String, recipient: String, message: String)
  case class ReceivedMessage(author: String, message: String)
  case class ReceivedDirectMessage(author: String, message: String)
  case class NoSuchMembers(username: String)
  case object MessageSent

  object JoinRoomResults {
    sealed trait JoinRoomResult
    case class SuccessfullyJoined(members: Iterable[String]) extends JoinRoomResult
    case class AlreadyJoined(username: String) extends JoinRoomResult
  }
}
