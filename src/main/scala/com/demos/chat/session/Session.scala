package com.demos.chat.session

import java.util.UUID

import akka.actor.{Actor, ActorRef, Props, Terminated}
import com.demos.chat._
import com.demos.chat.room.ChatRoom.JoinRoomResults.SuccessfullyJoined
import com.demos.chat.room.ChatRoom.{JoinRoom, ReceivedMessage, SendMessageToAll}
import com.demos.chat.session.Session.InitializeSession
import com.demos.chat.session.SessionRepository.Login
import com.demos.chat.session.SessionRepository.LoginResults.{LoginResult, LoginSuccessful}
import com.demos.chat.user.UserRepository.RegistrationResults.{RegistrationResult, RegistrationSuccessful}
import com.demos.chat.user.UserRepository.Register

/**
  *
  *
  * @author demos
  * @version 1.0
  */
class Session(id: UUID, gateway: ActorRef) extends Actor {

  override def receive: Receive = notInitialized()

  def notInitialized(): Receive = {
    case InitializeSession(connectionActor) =>
      context watch connectionActor
      context become anonymous(connectionActor)
  }

  def anonymous(connectionActor: ActorRef): Receive = {

    case RegistrationRequest(username, password) => gateway ! Register(username, password)
    case RegistrationSuccessful => connectionActor ! OkResponse
    case registrationError: RegistrationResult => connectionActor ! ErrorResponse(registrationError.toString)

    case LoginRequest(username, password) => gateway ! Login(id, username, password)
    case LoginSuccessful(username) =>
      context become authorized(connectionActor, username)
      connectionActor ! OkResponse
    case loginError: LoginResult => connectionActor ! ErrorResponse(loginError.toString)

    case Terminated(session) => context stop self

    case _ => connectionActor ! ErrorResponse("Invalid state")
  }

  def authorized(connectionActor: ActorRef, username: String): Receive = {

    case JoinRoomRequest() => gateway ! JoinRoom(username)
    case SuccessfullyJoined =>
      context become joined(connectionActor, username)
      connectionActor ! OkResponse

    case Terminated(session) => context stop self

    case _ => connectionActor ! ErrorResponse("Invalid state")
  }

  def joined(connectionActor: ActorRef, username: String): Receive = {

    case SendMessageToAllRequest(message) => gateway ! SendMessageToAll(username, message)
    case ReceivedMessage(author, message) => connectionActor ! ReceivedMessageResponse(author, message)

    case Terminated(session) => context stop self

    case _ => connectionActor ! ErrorResponse("Invalid state")
  }
}

object Session {
  def props(id: UUID, gateway: ActorRef) = Props(new Session(id, gateway))
  case class InitializeSession(connectionActor: ActorRef)
}
