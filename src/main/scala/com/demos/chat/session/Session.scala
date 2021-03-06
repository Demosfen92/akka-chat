package com.demos.chat.session

import akka.actor.{Actor, ActorRef, Props}
import com.demos.chat.messages._
import com.demos.chat.room.ChatRoom.JoinRoomResults.{JoinRoomResult, SuccessfullyJoined}
import com.demos.chat.room.ChatRoom._
import com.demos.chat.session.SessionRepository.Login
import com.demos.chat.session.SessionRepository.LoginResults.{LoginResult, LoginSuccessful}
import com.demos.chat.user.UserRepository.Register
import com.demos.chat.user.UserRepository.RegistrationResults.{RegistrationResult, RegistrationSuccessful}

/**
  * Session actor. Defines allowed messages per session state.
  *
  * anonymous - requires client to login
  * authorized - requires client to join chat room
  * joined - allows client to send and receive messages from other clients.
  */
class Session(connectionActor: ActorRef, gateway: ActorRef) extends Actor {

  override def receive: Receive = anonymous(connectionActor)

  def anonymous(connectionActor: ActorRef): Receive = {

    case RegistrationRequest(username, password) => gateway ! Register(username, password)
    case RegistrationSuccessful                  => connectionActor ! OkResponse
    case registrationError: RegistrationResult   => connectionActor ! ErrorResponse(registrationError.toString)

    case LoginRequest(username, password) => gateway ! Login(username, password)
    case LoginSuccessful(username)        =>
      context become authorized(connectionActor, username)
      connectionActor ! OkResponse
    case loginError: LoginResult          => connectionActor ! ErrorResponse(loginError.toString)

    case _ => connectionActor ! ErrorResponse("Invalid state")
  }

  def authorized(connectionActor: ActorRef, username: String): Receive = {

    case JoinRoomRequest()           => gateway ! JoinRoom(username)
    case SuccessfullyJoined(members) =>
      context become joined(connectionActor, username)
      connectionActor ! JoinedResponse(members)
    case joinError: JoinRoomResult   => connectionActor ! ErrorResponse(joinError.toString)

    case _ => connectionActor ! ErrorResponse("Invalid state")
  }

  def joined(connectionActor: ActorRef, ownerName: String): Receive = {

    case MemberJoined(username)       => connectionActor ! MemberJoinedResponse(username)
    case MemberDisconnected(username) => connectionActor ! MemberDisconnectedResponse(username)

    case SendMessageToAllRequest(message)             => gateway ! SendMessageToAll(ownerName, message)
    case SendDirectMessageRequest(recipient, message) => gateway ! SendDirectMessage(ownerName, recipient, message)
    case MessageSent                                  => connectionActor ! OkResponse
    case NoSuchMembers(username)                      => connectionActor ! ErrorResponse(s"There is no '$username' in a chat room")

    case ReceivedMessage(author, message)       => connectionActor ! ReceivedMessageResponse(author, message)
    case ReceivedDirectMessage(author, message) => connectionActor ! ReceivedDirectMessageResponse(author, message)

    case _ => connectionActor ! ErrorResponse("Invalid state")
  }
}

object Session {
  def props(connectionActor: ActorRef, gateway: ActorRef) = Props(new Session(connectionActor, gateway))
}