package com.demos.chat

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import com.demos.chat.ConnectionActor.{Connected, ConnectionClosed}
import com.demos.chat.messages.{ChatRequest, ChatResponse, ErrorResponse, JsonUtils}
import io.circe.parser.decode

import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn

/**
  * Entry point for the whole application.
  */
object ChatApplication extends JsonUtils {

  implicit val system: ActorSystem = ActorSystem("akka-chat-server")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  def main(args: Array[String]): Unit = {

    val gateway = system.actorOf(Gateway.props(), "gateway")

    val routes = helloRoute ~ chatRoute(gateway)
    val bindingFuture = Http().bindAndHandle(routes, "127.0.0.1", 8080)

    println("RETURN to stop...")
    StdIn.readLine()

    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())
  }

  def helloRoute: Route =
    path("hello") {
      get {
        complete("Hello")
      }
    }

  def chatRoute(gateway: ActorRef): Route = {
    path("chat") {
      get {
        handleWebSocketMessages(chatFlow(gateway))
      }
    }
  }

  private def chatFlow(gateway: ActorRef): Flow[Message, Message, NotUsed] = {

    val connectionActor = system.actorOf(ConnectionActor.props(gateway))

    val incomingMessages: Sink[Message, NotUsed] = Flow[Message]
      .map {
        case TextMessage.Strict(messageText) => decode[ChatRequest](messageText) match {
          case Right(request) => request
          case Left(error)    => ErrorResponse(error.getMessage)
        }
        case _                               => ErrorResponse("Unsupported operation")
      }
      .to(Sink.actorRef(connectionActor, ConnectionClosed))

    val outgoingMessages: Source[Message, NotUsed] =
      Source.actorRef[ChatResponse](10, OverflowStrategy.fail)
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