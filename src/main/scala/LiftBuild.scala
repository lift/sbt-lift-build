package net.liftweb

import sbt._
import Keys._

object LiftSbt extends Plugin {
  
  val buildOrganization = "net.liftweb"
  val buildVersion      = "2.4-SNAPSHOT"
  val buildScalaVersion = "2.8.1" //"2.9.0-1"

  val JavaNet2Repository = "java.net Maven2 Repository" at "http://download.java.net/maven/2"

  lazy val liftSettings = Seq(
    organization := buildOrganization,
    version      := buildVersion,
    scalaVersion := buildScalaVersion,
    // crossScalaVersions := Seq("2.8.0", "2.8.1", "2.9.0", "2.9.0-1"),
    resolvers     ++= Seq(JavaNet2Repository, ScalaToolsSnapshots), // TODO: make snapshot optional
    scalacOptions ++= Seq("-encoding", "UTF-8", /*"-deprecation", */"-unchecked")
  )

}
