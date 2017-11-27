package com.demos.chat.room

import akka.actor.{Actor, ActorRef, Props, Terminated}
import com.demos.chat.room.ChatRoom.JoinRoomResults.{AlreadyJoined, SuccessfullyJoined}
import com.demos.chat.room.ChatRoom._

/**
  *
  *
  * @author demos
  * @version 1.0
  */
class ChatRoom extends Actor {

  override def receive: Receive = chatRoom(Map.empty)

  def chatRoom(members: Map[ActorRef, String]): Receive = {
    case JoinRoom(username) => members.get(sender()) match {
      case None =>
        context watch sender()
        val updatedMembers = members + (sender() -> username)
        context become chatRoom(updatedMembers)
        sender() ! SuccessfullyJoined(updatedMembers.values)
        members foreach {
          case (recipient, _) => recipient ! MemberJoined(username)
        }
      case Some(_) => sender() ! AlreadyJoined(username)
    }
    case SendMessageToAll(author, message) =>
      members foreach {
        case (recipient, _) => recipient ! ReceivedMessage(author, message)
      }
    case SendDirectMessage(author, recipientName, message) =>
      members.find(_._2 == recipientName) match {
        case Some((recipient, _)) =>
          recipient ! ReceivedDirectMessage(author, message)
          sender() ! MessageSent
        case None => sender() ! NoSuchMembers(recipientName)
      }
    case Terminated(_) =>
      val updatedMembers = members - sender()
      context become chatRoom(updatedMembers)
      val username = members(sender())
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
