package com.demos.chat

/**
  *
  *
  * @author demos
  * @version 1.0
  */
sealed abstract class ChatResponse
case class ResponseMessage(message: String) extends ChatResponse
case class Error(message: String) extends ChatResponse
