package com.demos.chat.user

import akka.actor.{Actor, ActorRef, Props}
import com.demos.chat.session.SessionRepository.LoginWithSecret
import com.demos.chat.user.UserRepository.RegistrationResults._
import com.demos.chat.user.UserRepository.{GetSecret, Register}

/**
  * Actor that stores registered users.
  */
class UserRepository extends Actor {

  override def receive: Receive = userRepository(Map.empty)

  def userRepository(users: Map[String, String]): Receive = {
    case Register(username, password)           =>
      ((username, password) match {
        case (name, _) if name.isEmpty              => Left(InvalidUsername)
        case (_, pass) if pass.isEmpty              => Left(InvalidPassword)
        case (name, _) if users.get(name).isDefined => Left(UserAlreadyExists)
        case message                                => Right(message)
      }) match {
        case Right((user, pass)) =>
          context become userRepository(users + (user -> pass))
          sender() ! RegistrationSuccessful
        case Left(failedResult)  => sender() ! failedResult
      }
    case GetSecret(username, password, replyTo) =>
      sender() ! LoginWithSecret(username, password, users.get(username), replyTo)
  }
}

object UserRepository {

  def props() = Props(new UserRepository())

  case class Register(username: String, password: String)
  case class GetSecret(username: String, password: String, replyTo: ActorRef)

  object RegistrationResults {
    sealed trait RegistrationResult
    case object RegistrationSuccessful extends RegistrationResult
    case object UserAlreadyExists extends RegistrationResult
    case object InvalidUsername extends RegistrationResult
    case object InvalidPassword extends RegistrationResult
  }
}