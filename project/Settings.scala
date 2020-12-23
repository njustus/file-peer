import sbt._
import sbt.Keys._

object Settings {
  def settings = Seq(
    scalaVersion := "2.13.4",
    scalacOptions ++= Seq(
      "-unchecked",
      "-deprecation",
      "-feature",
      "-explaintypes",
      "-encoding", "utf8",
      "-language:higherKinds",
      "-language:postfixOps",
      "-Xfatal-warnings",
      "-Ymacro-annotations"
    )
  )

  def latestCommitHash():String = {
    import scala.sys.process._
    ("git rev-parse HEAD" !!).take(8).trim
  }

  def consoleInit:String = """
import filepeer.core._
import filepeer.core.discovery._
import filepeer.core.transfer._
import better.files._
import akka.stream._
import akka.stream.scaladsl._
"""
}

object Dependencies {

  val circeVersion = "0.12.3"

  val utils = Seq(
    "com.github.pureconfig" %% "pureconfig" % "0.14.0",
    "org.typelevel" %% "cats-core" % "2.2.0",
    "com.github.pathikrit" %% "better-files" % "3.9.1",
    "io.scalaland" %% "chimney" % "0.6.1",
    "ch.qos.logback" % "logback-classic" % "1.2.3",
    "com.softwaremill.macwire" %% "macros" % "2.3.3",
    // "org.rogach" %% "scallop" % "3.3.+",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
    "io.circe" %% "circe-core" % circeVersion,
    "io.circe" %% "circe-generic" % circeVersion,
    "io.circe" %% "circe-parser" % circeVersion,
    "org.scalatest" %% "scalatest" % "3.2.2" % Test,
    "org.apache.commons" % "commons-lang3" % "3.11" % Test
  )

  val akkaVersion = "2.6.10"
  val akka = Seq(
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion
  )

  val deps = utils ++ akka

}
