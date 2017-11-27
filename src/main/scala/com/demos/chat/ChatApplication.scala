package com.demos.chat

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import com.demos.chat.ConnectionActor.{Connected, ConnectionClosed}
import io.circe.parser.decode

import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn

/**
  * Entry point for the whole application.
  *
  * @author demos
  * @version 1.0
  */
object ChatApplication extends JsonUtils {

  implicit val system: ActorSystem = ActorSystem("akka-chat-server")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  def main(args: Array[String]): Unit = {

    val gateway = system.actorOf(Gateway.props(), "gateway")

    val routes = helloRoute ~ simpleWebSocketRoute ~ chatRoute(gateway)
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

  def chatRoute(gateway: ActorRef) = {
    path("chat") {
      get {
        handleWebSocketMessages(chatFlow(gateway))
      }
    }
  }

  private def chatFlow(gateway: ActorRef): Flow[Message, Message, NotUsed] = {

    val connectionActor = system.actorOf(ConnectionActor.props(gateway))

    val incomingMessages: Sink[Message, NotUsed] =
      Flow[Message].map {
        case TextMessage.Strict(messageText) =>
          decode[ChatRequest](messageText) match {
            case Right(request) => request
            case Left(error) => ErrorResponse(error.getMessage)
          }
        case _ => ErrorResponse("Unsupported operation")
      }.to(Sink.actorRef(connectionActor, ConnectionClosed))

    val outgoingMessages: Source[Message, NotUsed] =
      Source
        .actorRef[ChatResponse](10, OverflowStrategy.fail)
        .mapMaterializedValue { webSocketActor =>
          connectionActor ! Connected(webSocketActor)
          NotUsed
        }
        .map {
          response: ChatResponse => response.toTextMessage
        }

    Flow.fromSinkAndSource(incomingMessages, outgoingMessages)
  }
}
