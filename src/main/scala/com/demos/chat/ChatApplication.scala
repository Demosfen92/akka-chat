package com.demos.chat

import akka.NotUsed
import akka.actor.{ActorSystem, PoisonPill}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import io.circe.parser.decode

import scala.io.StdIn

/**
  * Entry point for the whole application.
  *
  * @author demos
  * @version 1.0
  */
object ChatApplication extends JsonUtils {

  implicit val system = ActorSystem("akka-chat-server")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  def main(args: Array[String]): Unit = {

    val routes = helloRoute ~ simpleWebSocketRoute ~ chatRoute
    val bindingFuture = Http().bindAndHandle(routes, "127.0.0.1", 8080)

    println("RETURN to stop...")
    StdIn.readLine()

    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())
  }

  def helloRoute =
    path("hello") {
      get {
        complete("Hello")
      }
    }

  def simpleWebSocketRoute =
    path("ws-simple") {
      get {
        val echoFlow: Flow[Message, Message, _] = Flow[Message].map {
          case TextMessage.Strict(text) => TextMessage(s"Received message: $text!")
          case _ => TextMessage("Unsupported operation")
        }
        handleWebSocketMessages(echoFlow)
      }
    }

  def chatRoute =
    path("chat") {
      handleWebSocketMessages(chatFlow())
    }

  private def chatFlow(): Flow[Message, Message, NotUsed] = {

    val connectionActor = system.actorOf(ConnectionActor.props())

    val incomingMessages: Sink[Message, NotUsed] =
      Flow[Message].map {
        case TextMessage.Strict(messageText) =>
          decode[ChatRequest](messageText) match {
            case Right(request) => connectionActor ! request
            case Left(error) => connectionActor ! Error(error.getMessage)
          }
        case _ => connectionActor ! Error("Unsupported operation")
      }.to(Sink.actorRef(connectionActor, PoisonPill))

    val outgoingMessages: Source[Message, NotUsed] =
      Source
        .actorRef[ChatResponse](10, OverflowStrategy.fail)
        .mapMaterializedValue { webSocketActor =>
          connectionActor ! ConnectionActor.Connected(webSocketActor)
          NotUsed
        }
        .map {
          case response: ChatResponse => response.toTextMessage
        }

    Flow.fromSinkAndSource(incomingMessages, outgoingMessages)
  }
}
