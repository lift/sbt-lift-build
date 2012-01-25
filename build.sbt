sbtPlugin := true

organization := "net.liftweb"

name := "lift-sbt-plugin"

//(version in Lift) := "0.0.1"

version := "0.0.1"

licenses += ("Apache License, Version 2.0", url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

scalacOptions                    ++= DefaultOptions.scalac

scalacOptions in compile          += Opts.compile.deprecation

publishArtifact in (Compile, packageDoc) := false

publishArtifact in (Compile, packageSrc) := false
