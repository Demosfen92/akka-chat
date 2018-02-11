package com.demos.chat.session

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.demos.chat.messages._
import com.demos.chat.room.ChatRoom._
import com.demos.chat.room.ChatRoom.JoinRoomResults.{AlreadyJoined, SuccessfullyJoined}
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
  val joinedMessage = "joinedMessage"

  "Anonymous session must" must {

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
      session ! RegistrationRequest("user1", "pass1")
      gateway.expectMsg(Register("user1", "pass1"))
      gateway.reply(RegistrationSuccessful)
      connectionActor.expectMsg(OkResponse)
    }

    "return error response in case of failed registration" in new AnonymousScope {
      session ! RegistrationRequest("user1", "pass1")
      gateway.expectMsg(Register("user1", "pass1"))
      gateway.reply(UserAlreadyExists)
      connectionActor.expectMsg(ErrorResponse(UserAlreadyExists.toString))
    }

    "return ok response and change the state to authorize in case of successful login" in new AnonymousScope {
      session ! LoginRequest("user1", "pass1")
      gateway.expectMsg(Login("user1", "pass1"))
      gateway.reply(LoginSuccessful("user1"))
      connectionActor.expectMsg(OkResponse)
      session ! authorizedMessage
      expectMsg(authorizedMessage)
    }

    "return error response and don't the change state in case of failed login" in new AnonymousScope {
      session ! LoginRequest("user1", "pass1")
      gateway.expectMsg(Login("user1", "pass1"))
      gateway.reply(IncorrectPassword("user1"))
      connectionActor.expectMsg(ErrorResponse(IncorrectPassword("user1").toString))
      session ! anonymousMessage
      expectMsg(anonymousMessage)
    }
  }

  "Authorized session" must {

    trait AuthorizedScope {
      val connectionActor = TestProbe()
      val gateway = TestProbe()
      val username = "username"
      val session: ActorRef = system.actorOf(Props(new Session(connectionActor.ref, gateway.ref) with StabEchoReceiver {

        override def receive: Receive = authorized(connectionActor.ref, username)

        override def authorized(connectionActor: ActorRef, username: String): Receive =
          stabReceive(authorizedMessage) orElse super.authorized(connectionActor, username)

        override def joined(connectionActor: ActorRef, ownerName: String): Receive =
          stabReceive(joinedMessage) orElse super.joined(connectionActor, ownerName)
      }))
    }

    "return joined response and change the state to joined in case of successful join to the room" in new AuthorizedScope {
      session ! JoinRoomRequest()
      gateway.expectMsg(JoinRoom(username))
      gateway.reply(SuccessfullyJoined(List("user1", "user2")))
      connectionActor.expectMsg(JoinedResponse(List("user1", "user2")))
      session ! joinedMessage
      expectMsg(joinedMessage)
    }

    "return error response and don't change the state in case if already joined" in new AuthorizedScope {
      session ! JoinRoomRequest()
      gateway.expectMsg(JoinRoom(username))
      gateway.reply(AlreadyJoined(username))
      connectionActor.expectMsg(ErrorResponse(AlreadyJoined(username).toString))
      session ! authorizedMessage
      expectMsg(authorizedMessage)
    }
  }

  "Joined session" must {

    trait JoinedScope {
      val connectionActor = TestProbe()
      val gateway = TestProbe()
      val username = "username"
      val session: ActorRef = system.actorOf(Props(new Session(connectionActor.ref, gateway.ref) with StabEchoReceiver {
        override def receive: Receive = joined(connectionActor.ref, username)
      }))
    }

    "forward member joined response to the client" in new JoinedScope {
      session ! MemberJoined(username)
      connectionActor.expectMsg(MemberJoinedResponse(username))
    }

    "forward member disconnected response to the client" in new JoinedScope {
      session ! MemberDisconnected(username)
      connectionActor.expectMsg(MemberDisconnectedResponse(username))
    }

    "send message to everyone in a room and receive this message back" in new JoinedScope {
      session ! SendMessageToAllRequest("message")
      gateway.expectMsg(SendMessageToAll(username, "message"))
      gateway.reply(ReceivedMessage(username, "message"))
      connectionActor.expectMsg(ReceivedMessageResponse(username, "message"))
    }

    "send direct message and return message sent response" in new JoinedScope {
      session ! SendDirectMessageRequest("recipient", "message")
      gateway.expectMsg(SendDirectMessage(username, "recipient", "message"))
      gateway.reply(MessageSent)
      connectionActor.expectMsg(OkResponse)
    }

    "send direct message and return no such members response if there are no such recipients" in new JoinedScope {
      session ! SendDirectMessageRequest("recipient", "message")
      gateway.expectMsg(SendDirectMessage(username, "recipient", "message"))
      gateway.reply(NoSuchMembers("recipient"))
      connectionActor.expectMsg(ErrorResponse("There is no 'recipient' in a chat room"))
    }

    "forward direct messages to the client" in new JoinedScope {
      session ! ReceivedDirectMessage("author", "message")
      connectionActor.expectMsg(ReceivedDirectMessageResponse("author", "message"))
    }
  }

  override protected def afterAll(): Unit = TestKit.shutdownActorSystem(system)
}

trait StabEchoReceiver { self: Actor =>
  def stabReceive(state: String): Receive = {
    case message: String if message == state => sender() ! state
  }
}
