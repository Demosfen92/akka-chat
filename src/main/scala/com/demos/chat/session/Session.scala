package com.demos.chat.session

import java.util.UUID

import akka.actor.{Actor, ActorRef, Props}
import com.demos.chat._
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
    case InitializeSession(connectionActor) => context become anonymous(connectionActor)
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

    case _ => connectionActor ! ErrorResponse("Unexpected message")
  }

  def authorized(connectionActor: ActorRef, username: String): Receive = ???
}

object Session {
  def props(id: UUID, gateway: ActorRef) = Props(new Session(id, gateway))
  case class InitializeSession(connectionActor: ActorRef)
}
