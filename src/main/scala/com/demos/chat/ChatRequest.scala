package com.demos.chat

/**
  *
  *
  * @author demos
  * @version 1.0
  */
sealed trait ChatRequest
case class HeartBeat() extends ChatRequest
case class RegistrationRequest(username: String, password: String) extends ChatRequest
case class LoginRequest(username: String, password: String) extends ChatRequest
case class JoinRoomRequest() extends ChatRequest
case class SendMessageToAllRequest(message: String) extends ChatRequest
