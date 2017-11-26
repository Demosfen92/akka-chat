package com.demos.chat.session

import java.util.UUID

import akka.actor.{Actor, ActorRef, Props, Terminated}
import com.demos.chat.session.SessionRepository.LoginResults._
import com.demos.chat.session.SessionRepository.{Login, LoginWithSecret, StartSession}
import com.demos.chat.user.UserRepository.GetSecret

/**
  *
  *
  * @author demos
  * @version 1.0
  */
class SessionRepository(gateway: ActorRef) extends Actor {

  override def receive: Receive = sessionRepository(Map.empty)

  def sessionRepository(sessions: Map[String, ActorRef]): Receive = {
    case StartSession => sender() ! context.actorOf(Session.props(UUID.randomUUID(), gateway))
    case Login(id, username, password) => sessions.get(username) match {
      case Some(_) => sender() ! AlreadyIn(username)
      case None => gateway ! GetSecret (id, username, password, sender())
    }
    case LoginWithSecret(id, username, password, secret, replyTo) => secret match {
      case None => replyTo ! UserNotExists(username)
      case Some(storedPassword) if storedPassword != password => replyTo ! IncorrectPassword(username)
      case _ =>
        context watch replyTo
        context become sessionRepository(sessions + (username -> replyTo))
        replyTo ! LoginSuccessful(username)
    }
    case Terminated(session) =>
      val username = sessions.find(_._2 == session).get._1
      println(s"$username disconnected")
      context become sessionRepository(sessions - username)
  }
}

object SessionRepository {

  def props(gateway: ActorRef) = Props(new SessionRepository(gateway))

  case object StartSession
  case class Login(sessionId: UUID, username: String, password: String)
  case class LoginWithSecret(sessionId: UUID, username: String, password: String, secret: Option[String], replyTo: ActorRef)

  object LoginResults {
    sealed trait LoginResult
    case class AlreadyIn(username: String) extends LoginResult
    case class IncorrectPassword(username: String) extends LoginResult
    case class UserNotExists(username: String) extends LoginResult
    case class LoginSuccessful(username: String) extends LoginResult
  }
}
