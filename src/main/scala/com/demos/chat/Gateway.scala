package com.demos.chat

import akka.actor.{Actor, Props}
import akka.util.Timeout
import com.demos.chat.session.SessionRepository
import com.demos.chat.session.SessionRepository.{Login, StartSession}
import com.demos.chat.user.UserRepository
import com.demos.chat.user.UserRepository.{GetSecret, Register}

import scala.concurrent.ExecutionContextExecutor

/**
  *
  *
  * @author demos
  * @version 1.0
  */
class Gateway(implicit val dispatcher: ExecutionContextExecutor, implicit val timeout: Timeout) extends Actor {

  private val sessionRepository = context.actorOf(SessionRepository.props(self))
  private val userRepository = context.actorOf(UserRepository.props())

  override def receive: Receive = {

    case message @ StartSession => sessionRepository.forward(message)
    case message @ Login(_, _, _) => sessionRepository.forward(message)

    case message @ Register(_, _) => userRepository.forward(message)
    case message @ GetSecret(_) => userRepository.forward(message)
  }

}

object Gateway {
  def props()(implicit dispatcher: ExecutionContextExecutor, timeout: Timeout) = Props(new Gateway())
}
