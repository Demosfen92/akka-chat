package com.demos.chat.session

import java.util.UUID

import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.demos.chat.session.SessionRepository.LoginResults._
import com.demos.chat.session.SessionRepository.{Login, StartSession}
import com.demos.chat.user.UserRepository.GetSecret

import scala.concurrent.ExecutionContextExecutor
import scala.util.Success

/**
  *
  *
  * @author demos
  * @version 1.0
  */
class SessionRepository(gateway: ActorRef)(implicit val dispatcher: ExecutionContextExecutor,
                                           implicit val timeout: Timeout) extends Actor {

  override def receive: Receive = sessionRepository(Map.empty)

  def sessionRepository(sessions: Map[String, UUID]): Receive = {
    case StartSession => sender() ! context.actorOf(Session.props(UUID.randomUUID(), gateway))
    case Login(id, username, password) => sessions.get(username) match {
      case Some(_) => sender() ! AlreadyLogin(username)
      case None => gateway.ask(GetSecret(username)).mapTo[Option[String]].map {
        case None => UserNotExists(username)
        case Some(storedPassword) if storedPassword != password => IncorrectPassword(username)
        case _ =>
          context become sessionRepository(sessions + (username -> id))
          LoginSuccessful(username)
      }.onComplete {
        case Success(result) => sender() ! result
        case _ => sender() ! PersistenceError
      }
    }

  }
}

object SessionRepository {

  def props(gateway: ActorRef)(implicit dispatcher: ExecutionContextExecutor, timeout: Timeout) = Props(new SessionRepository(gateway))

  case object StartSession
  case class Login(sessionId: UUID, username: String, password: String)

  object LoginResults {
    sealed trait LoginResult
    case class AlreadyLogin(username: String) extends LoginResult
    case class IncorrectPassword(username: String) extends LoginResult
    case class UserNotExists(username: String) extends LoginResult
    case object PersistenceError extends LoginResult
    case class LoginSuccessful(username: String) extends LoginResult
  }
}
