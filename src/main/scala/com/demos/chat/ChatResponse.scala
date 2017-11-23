package com.demos.chat

/**
  *
  *
  * @author demos
  * @version 1.0
  */
sealed trait ChatResponse
case class ErrorResponse(message: String) extends ChatResponse
case object OkResponse extends ChatResponse
