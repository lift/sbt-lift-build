/*
 * Copyright 2011 WorldWide Conferencing, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.liftweb.sbt

import sbt._
import Keys._

sealed trait LiftDefaults {

  val SonatypeSnapshots = "Sonatype Nexus Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
  val SonatypeStaging   = "Sonatype Nexus Staging" at "https://oss.sonatype.org/service/local/staging/deploy/maven2/"

  lazy val liftDefaultSettings: Seq[Setting[_]] = Seq(
    name ~= formalize,

    javacOptions                    ++= DefaultOptions.javac,
    javacOptions in compile          += Opts.compile.deprecation,
    javacOptions in (Compile, doc) <++= (name in (Compile, doc), version in (Compile, doc)) apply javadoc,

    scalacOptions                    ++= DefaultOptions.scalac,
    scalacOptions in compile          += Opts.compile.deprecation,
    scalacOptions in (Compile, doc) <++= (name in (Compile, doc), version in (Compile, doc)) map DefaultOptions.scaladoc,

    resolvers <++= isSnapshot { s => if (s) Seq(SonatypeSnapshots) else Nil },

    shellPrompt <<= (state, version)((s, v) => { s => "sbt:%s:%s> ".format(Project.extract(s).currentProject.id, v) }),
    initialCommands in console := "import netliftweb._;",
  )

  def formalize(name: String) = name.split("-") map(_.capitalize) mkString(" ")

  // TODO: "Use `DefaultOptions.javadoc` instead" and consider javacOptions as task (like scalacOptions)
  def javadoc(name: String, version: String): Seq[String] = Seq("-doctitle", "%s %s API".format(name, version))

  def selectDynamic(default: String, alternatives: (String, String)*)(scalaVersion: String) =
    Map(alternatives: _*).getOrElse(scalaVersion, default)

}

object LiftAppPlugin extends Plugin with LiftDefaults {

  lazy val liftAppSettings = liftDefaultSettings ++ Seq(
    // startYear
    // initialCommands in console += """import netliftweb._
    //                                 |import common._
    //                                 |import json._""".stripMargin
  )
}

object LiftBuildPlugin extends Plugin with LiftDefaults {

  lazy val liftBuildSettings = liftDefaultSettings ++ Seq(
    organization     := "net.liftweb",
    licenses         += ("Apache License, Version & 2.0", url("http://www.apache.org/licenses/LICENSE-2.0.txt")),
    homepage         := Some(url("http://www.liftweb.net")),
    organizationName := "WorldWide Conferencing, LLC",
    startYear        := Some(2006),

    scalacOptions ++= Seq(/*"-unchecked"*/), // TODO: Pull up to LiftDefaults. Also should enable "-Xcheckinit", -Xwarninit" (in LiftBuildPlugin only) when things get in good order

    pomIncludeRepository := { _ => false },
    publishTo           <<= isSnapshot { s => Some(if (s) SonatypeSnapshots else SonatypeStaging) },
    credentials          += Credentials(Path.userHome / ".sbt" / ".credentials")
  )
}
