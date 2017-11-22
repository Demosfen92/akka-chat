package com.demos.chat

/**
  *
  *
  * @author demos
  * @version 1.0
  */
sealed abstract class ChatRequest
object HeartBeat extends ChatRequest
case class SimpleMessage(message: String) extends ChatRequest
