package com.demos.chat.session

import akka.actor.{Actor, ActorRef, Props, Terminated}
import com.demos.chat.session.SessionRepository.LoginResults._
import com.demos.chat.session.SessionRepository.{Login, LoginWithSecret}
import com.demos.chat.user.UserRepository.GetSecret

/**
  * Actor that stores currently authorized clients.
  */
class SessionRepository(gateway: ActorRef) extends Actor {

  override def receive: Receive = sessionRepository(Map.empty, Set.empty)

  def sessionRepository(sessions: Map[ActorRef, String], users: Set[String]): Receive = {

    case Login(username, password) =>
      if (users.contains(username)) {
        sender() ! AlreadyIn(username)
      } else {
        gateway ! GetSecret(username, password, sender())
      }

    case LoginWithSecret(username, password, secret, replyTo) =>
      secret match {
        case None                                               => replyTo ! UserNotExists(username)
        case Some(storedPassword) if storedPassword != password => replyTo ! IncorrectPassword(username)
        case _                                                  =>
          context watch replyTo
          context become sessionRepository(sessions + (replyTo -> username), users + username)
          replyTo ! LoginSuccessful(username)
      }

    case Terminated(session) =>
      val username = sessions(session)
      println(s"$username disconnected")
      context become sessionRepository(sessions - session, users - username)
  }
}

object SessionRepository {

  def props(gateway: ActorRef) = Props(new SessionRepository(gateway))

  case class Login(username: String, password: String)
  case class LoginWithSecret(username: String, password: String, secret: Option[String], replyTo: ActorRef)

  object LoginResults {
    sealed trait LoginResult
    case class AlreadyIn(username: String) extends LoginResult
    case class IncorrectPassword(username: String) extends LoginResult
    case class UserNotExists(username: String) extends LoginResult
    case class LoginSuccessful(username: String) extends LoginResult
  }
}