package com.demos.chat.messages

/**
  * Incoming messages.
  */
sealed trait ChatRequest
case class HeartBeat() extends ChatRequest
case class RegistrationRequest(username: String, password: String) extends ChatRequest
case class LoginRequest(username: String, password: String) extends ChatRequest
case class JoinRoomRequest() extends ChatRequest
case class SendMessageToAllRequest(message: String) extends ChatRequest
case class SendDirectMessageRequest(recipient: String, message: String) extends ChatRequest