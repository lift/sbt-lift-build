/*
 * Copyright 2011-2012 WorldWide Conferencing, LLC
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

  object PublishRepo {
    lazy val Local    = Resolver.file("Local Repository", Path.userHome / ".m2" / "repository" asFile)
    lazy val Snapshot = "Sonatype Nexus Repository for Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
    lazy val Staging  = "Sonatype Nexus Repository for Staging" at "https://oss.sonatype.org/service/local/staging/deploy/maven2/"
  }

  lazy val SnapshotResolver = "Sonatype Repository for Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

  lazy val baseSettings: Seq[Setting[_]] = Seq(
    name           ~= formalize,
    javacOptions  ++= DefaultOptions.javac,
    scalacOptions ++= DefaultOptions.scalac :+ Opts.compile.deprecation, // :+ Opts.compile.unchecked, // FIXME: breaks tests
    packageOptions += Package.ManifestAttributes("Built-By"   -> System.getProperty("user.name", "unknown"),
                                                 "Built-Time" -> java.util.Calendar.getInstance.getTimeInMillis.toString),
    resolvers     <<= isSnapshot(if (_) Seq(SnapshotResolver) else Nil),
    shellPrompt   <<= version(v => s => "%s:%s:%s> ".format(s.configuration.provider.id.name, Project.extract(s).currentProject.id, v)),
    // DefaultOptions.setupShellPrompt, // TODO: use this instead with 0.12.0-M2
    initialCommands in console := "import net.liftweb._;"
  )

  lazy val compileSettings: Seq[Setting[_]] = inTask(compile)(Seq(
    javacOptions  += Opts.compile.deprecation,
    scalacOptions += "-Xcheckinit"
  ))

  lazy val docSettings: Seq[Setting[_]] = inTask(doc)(Seq(
    javacOptions  <++= (name, version) map DefaultOptions.javadoc,
    scalacOptions <++= (name, version) map DefaultOptions.scaladoc
  ))

  lazy val liftDefaultSettings: Seq[Setting[_]] =
    baseSettings ++ Seq(Compile, Test, IntegrationTest).flatMap(inConfig(_)(compileSettings ++ docSettings))

  def formalize(name: String): String = name.split("-") map (_.capitalize) mkString (" ")

  // Helper for Aggregated doc
  def aggregatedSetting[T](taskKey: TaskKey[Seq[T]]): Setting[_] =
    taskKey <<= Defaults.inDependencies[Task[Seq[T]]](taskKey.task, _ => task(Nil), aggregate = true) apply { _.join.map(_.flatten) }

  def crossMapped(mappings: (String, String)*): CrossVersion =
    CrossVersion.binaryMapped(Map(mappings: _*) orElse { case v => v })

  def defaultOrMapped(default: String, alternatives: (String, String)*): String => String =
    Map(alternatives: _*) orElse { case _ => default }

  def printLogo(name: String, version: String) {
    import scala.Console.{BLUE, GREEN, RESET}
    if (!java.lang.Boolean.getBoolean("sbt.lift.nologo")) {
      // TODO: improve this quick hack
      def logo(col1: String, col2: String, reset: String = RESET) =
        """ %s
          |   _     _  __ _
          |  | |   (_)/ _| |_    %s %s %s
          |  | |   | | |_| __|   %s %s %s
          |  | |___| |  _| |_
          |  |_____|_|_|  \__|
          | %s
          |""".stripMargin.format(col1, col2, name, col1, col2, version, col1, reset)

      println(if (ConsoleLogger.formatEnabled) logo(BLUE, GREEN) else logo("", "", ""))
    }
  }
}

object LiftBuildPlugin extends Plugin with LiftDefaults {
  lazy val liftBuildSettings = liftDefaultSettings
}
