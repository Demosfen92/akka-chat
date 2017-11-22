name := "akka-chat"

version := "1.0"

scalaVersion := "2.12.4"

lazy val akkaVersion = "2.5.6"
lazy val akkaHttpVersion = "10.0.10"
lazy val akkaStreamVersion = "2.5.6"
lazy val circeVersion = "0.8.0"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaStreamVersion,
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
//  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
//  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % "test",

  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-generic-extras" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion
)
        