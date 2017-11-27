package com.demos.chat

import akka.actor.{Actor, Props}
import com.demos.chat.room.ChatRoom
import com.demos.chat.room.ChatRoom.{JoinRoom, SendDirectMessage, SendMessageToAll}
import com.demos.chat.session.SessionRepository
import com.demos.chat.session.SessionRepository.Login
import com.demos.chat.user.UserRepository
import com.demos.chat.user.UserRepository.{GetSecret, Register}

/**
  *
  *
  * @author demos
  * @version 1.0
  */
class Gateway extends Actor {

  private val sessionRepository = context.actorOf(SessionRepository.props(self))
  private val userRepository = context.actorOf(UserRepository.props())
  private val chatRoom = context.actorOf(ChatRoom.props())

  override def receive: Receive = {

    case message @ Login(_, _) => sessionRepository.forward(message)

    case message @ Register(_, _) => userRepository.forward(message)
    case message @ GetSecret(_, _, _) => userRepository.forward(message)

    case message @ JoinRoom(_) => chatRoom.forward(message)
    case message @ SendMessageToAll(_, _) => chatRoom.forward(message)
    case message @ SendDirectMessage(_, _, _) => chatRoom.forward(message)
  }
}

object Gateway {
  def props() = Props(new Gateway())
}
