sbtPlugin := true

name := "lift-sbt-plugin"

organization := "net.liftweb"

//(version in Lift) := "0.0.1"

version := "0.0.1"

scalacOptions := Seq("-deprecation", "-unchecked")

publishTo <<= (version) { version: String =>
  val snapshot = "Nexus Repository for Snapshots" at "http://nexus.scala-tools.org/content/repositories/snapshots/"
  val release  = "Nexus Repository for Releases"  at "http://nexus.scala-tools.org/content/repositories/releases/"
  if (version endsWith "-SNAPSHOT") Some(snapshot) else Some(release)
}
