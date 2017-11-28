package com.demos.chat.session

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.demos.chat.messages.{ErrorResponse, LoginRequest, OkResponse, RegistrationRequest}
import com.demos.chat.session.SessionRepository.Login
import com.demos.chat.session.SessionRepository.LoginResults.{IncorrectPassword, LoginSuccessful}
import com.demos.chat.user.UserRepository.Register
import com.demos.chat.user.UserRepository.RegistrationResults.{RegistrationSuccessful, UserAlreadyExists}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

class SessionTest extends TestKit(ActorSystem("testSystem"))
  with WordSpecLike
  with BeforeAndAfterAll
  with ImplicitSender {

  val anonymousMessage = "anonymousMessage"
  val authorizedMessage = "authorizedMessage"

  "Session must" must {

    trait AnonymousScope {
      val connectionActor = TestProbe()
      val gateway = TestProbe()
      val session: ActorRef = system.actorOf(Props(new Session(connectionActor.ref, gateway.ref) with StabEchoReceiver {

        override def receive: Receive = anonymous(connectionActor.ref)

        override def anonymous(connectionActor: ActorRef): Receive =
          stabReceive(anonymousMessage) orElse super.anonymous(connectionActor)

        override def authorized(connectionActor: ActorRef, username: String): Receive =
          stabReceive(authorizedMessage) orElse super.authorized(connectionActor, username)
      }))
    }

    "return ok response in case of successful registration" in new AnonymousScope {
      session.tell(RegistrationRequest("user1", "pass1"), connectionActor.ref)
      gateway.expectMsg(Register("user1", "pass1"))
      gateway.reply(RegistrationSuccessful)
      connectionActor.expectMsg(OkResponse)
    }

    "return error response in case of failed registration" in new AnonymousScope {
      session.tell(RegistrationRequest("user1", "pass1"), connectionActor.ref)
      gateway.expectMsg(Register("user1", "pass1"))
      gateway.reply(UserAlreadyExists)
      connectionActor.expectMsg(ErrorResponse(UserAlreadyExists.toString))
    }

    "return ok response and change state to authorize in case of successful login" in new AnonymousScope {
      session.tell(LoginRequest("user1", "pass1"), connectionActor.ref)
      gateway.expectMsg(Login("user1", "pass1"))
      gateway.reply(LoginSuccessful("user1"))
      connectionActor.expectMsg(OkResponse)
      session ! authorizedMessage
      expectMsg(authorizedMessage)
    }

    "return error response and don't change state in case of failed login" in new AnonymousScope {
      session.tell(LoginRequest("user1", "pass1"), connectionActor.ref)
      gateway.expectMsg(Login("user1", "pass1"))
      gateway.reply(IncorrectPassword("user1"))
      connectionActor.expectMsg(ErrorResponse(IncorrectPassword("user1").toString))
      session ! anonymousMessage
      expectMsg(anonymousMessage)
    }
  }

  override protected def afterAll(): Unit = TestKit.shutdownActorSystem(system)
}

trait StabEchoReceiver { self: Actor =>
  def stabReceive(state: String): Receive = {
    case message: String if message == state => sender() ! state
  }
}
