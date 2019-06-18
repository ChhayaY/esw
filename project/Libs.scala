import sbt._

object Libs {
  val scalatest = "org.scalatest"    %% "scalatest" % "3.0.6"     //Apache License 2.0
  val `scopt`   = "com.github.scopt" %% "scopt"     % "4.0.0-RC2" //MIT License
}

object Csw {
  private val Org     = "com.github.tmtsoftware.csw"
  private val Version = "423c6c8" //change this to 0.1-SNAPSHOT to test with local csw changes (after publishLocal)

  val `csw-command-client` = Org %% "csw-command-client" % Version
}

object Akka {
  private val Version = "2.5.23" //all akka is Apache License 2.0

  val `akka-actor`          = "com.typesafe.akka" %% "akka-actor"          % Version
  val `akka-testkit`        = "com.typesafe.akka" %% "akka-testkit"        % Version
  val `akka-stream`         = "com.typesafe.akka" %% "akka-stream"         % Version
  val `akka-stream-testkit` = "com.typesafe.akka" %% "akka-stream-testkit" % Version 
}

object AkkaHttp {
  private val Version = "10.1.8" //all akka is Apache License 2.0

  val `akka-http`         = "com.typesafe.akka" %% "akka-http"         % Version
  val `akka-http-testkit` = "com.typesafe.akka" %% "akka-http-testkit" % Version
}
