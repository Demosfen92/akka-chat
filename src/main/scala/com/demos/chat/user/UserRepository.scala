package com.demos.chat.user

import akka.actor.{ActorRef, Props}
import akka.persistence.{PersistentActor, SnapshotOffer}
import com.demos.chat.session.SessionRepository.LoginWithSecret
import com.demos.chat.user.UserRepository.RegistrationResults._
import com.demos.chat.user.UserRepository.{GetSecret, Register, RegisteredEvent, UserRepositoryState}

/**
  * Persistent actor that stores registered users.
  */
class UserRepository extends PersistentActor {

  override def persistenceId: String = "akka-chat-user-repository"
  val snapshotInterval = 5

  var state = UserRepositoryState()

  def updateState(event: RegisteredEvent): Unit =
    state = state.updated(event)

  override def receiveRecover: Receive = {
    case evt: RegisteredEvent => updateState(evt)
    case SnapshotOffer(_, snapshot: UserRepositoryState) => state = snapshot
  }

  override def receiveCommand: Receive = {
    case Register(username, password)           =>
      ((username, password) match {
        case (name, _) if name.isEmpty                    => Left(InvalidUsername)
        case (_, pass) if pass.isEmpty                    => Left(InvalidPassword)
        case (name, _) if state.users.get(name).isDefined => Left(UserAlreadyExists)
        case message                                      => Right(message)
      }) match {
        case Right((user, pass)) =>
          val replyTo = sender()
          persist(RegisteredEvent(user, pass)) { event =>
            updateState(event)
            context.system.eventStream.publish(event)
            replyTo ! RegistrationSuccessful
            if (lastSequenceNr % snapshotInterval == 0 && lastSequenceNr != 0)
              saveSnapshot(state)
          }
        case Left(failedResult)  => sender() ! failedResult
      }
    case GetSecret(username, password, replyTo) =>
      sender() ! LoginWithSecret(username, password, state.users.get(username), replyTo)
  }
}

object UserRepository {

  def props() = Props(new UserRepository())

  case class UserRepositoryState(users: Map[String, String] = Map.empty) {
    def updated(evt: RegisteredEvent): UserRepositoryState = copy(users + (evt.username -> evt.password))
  }

  case class Register(username: String, password: String)
  case class GetSecret(username: String, password: String, replyTo: ActorRef)

  case class RegisteredEvent(username: String, password: String)

  object RegistrationResults {
    sealed trait RegistrationResult
    case object RegistrationSuccessful extends RegistrationResult
    case object UserAlreadyExists extends RegistrationResult
    case object InvalidUsername extends RegistrationResult
    case object InvalidPassword extends RegistrationResult
  }
}