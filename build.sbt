sbtPlugin    := true
organization := "net.liftweb"

name         := "sbt-lift-build-plugin"
version      := "0.0.1"
licenses     += ("Apache License, Version 2.0", url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

scalacOptions ++= DefaultOptions.scalac

Compile / scalacOptions += Opts.compile.deprecation
Compile / packageDoc / publishArtifact := false
Compile / packageSrc / publishArtifact := false
