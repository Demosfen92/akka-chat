package com.demos.chat.messages

/**
  * Outgoing messages.
  */
sealed trait ChatResponse
case class ErrorResponse(message: String) extends ChatResponse
case object OkResponse extends ChatResponse
case class JoinedResponse(members: Iterable[String]) extends ChatResponse
case class MemberJoinedResponse(username: String) extends ChatResponse
case class MemberDisconnectedResponse(username: String) extends ChatResponse
case class ReceivedMessageResponse(username: String, message: String) extends ChatResponse
case class ReceivedDirectMessageResponse(username: String, message: String) extends ChatResponse