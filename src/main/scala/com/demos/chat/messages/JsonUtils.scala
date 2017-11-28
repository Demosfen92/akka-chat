package com.demos.chat.messages

import akka.http.scaladsl.model.ws.TextMessage
import io.circe.generic.extras.{Configuration, semiauto => fancy}
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder}

/**
  * Trait with circe encoders/decoders for JSON.
  *
  * @author demos
  * @version 1.0
  */
trait JsonUtils {

  implicit val customConfig: Configuration =
    Configuration
      .default
      .withDefaults
      .withSnakeCaseKeys
      .withDiscriminator("$type")

  implicit val chatRequestDecoder: Decoder[ChatRequest] = fancy.deriveDecoder[ChatRequest]
  implicit val chatResponseEncoder: Encoder[ChatResponse] = fancy.deriveEncoder[ChatResponse]

  implicit class ChatResponseConverter(chatResponse: ChatResponse) {
    def toTextMessage: TextMessage = TextMessage.Strict(chatResponse.asJson.toString())
  }
}
