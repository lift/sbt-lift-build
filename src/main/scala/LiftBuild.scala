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

import scala.Console.{CYAN, BLUE, GREEN, RESET}
import sbt._
import sbt.internal.util.ConsoleAppender
import Keys._

sealed trait LiftDefaults {

  object Logo {
    lazy val Arrow = (""": ___,__
                         :|  / \  \
                         :|  | |  |
                         : \__\_\_.
                         :""".stripMargin(':'), CYAN)

    lazy val Text  = (""": _     _   __  _
                         :| |   (_)/  _|| |_
                         :| |__ | ||  _||  _|
                         :|____||_||_|   \__|
                         :""".stripMargin(':'), BLUE)

    lazy val About = (""":
                         :%s
                         :%s for Scala %s
                         :
                         :""".stripMargin(':'), GREEN)
  }

  lazy val baseSettings: Seq[Setting[_]] = Seq(
    name           ~= formalize,
    javacOptions  ++= DefaultOptions.javac,
    scalacOptions ++= DefaultOptions.scalac :+ Opts.compile.deprecation, // :+ Opts.compile.unchecked, // FIXME: breaks tests
    packageOptions += Package.ManifestAttributes("Built-By"   -> System.getProperty("user.name", "unknown"),
                                                 "Built-Time" -> java.util.Calendar.getInstance.getTimeInMillis.toString),
    DefaultOptions.addResolvers,
    DefaultOptions.setupShellPrompt
  )

  lazy val compileSettings: Seq[Setting[_]] = inTask(compile)(Seq(
    javacOptions  += Opts.compile.deprecation
    //scalacOptions += "-Xcheckinit"  // FIXME: breaks lift-util
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

  def defaultOrMapped(defaultValue: String, alternatives: (String, String)*): String => String =
    Map(alternatives: _*) orElse { case _ => defaultValue }

  // Logo printer
  def printLogo(name: String, version: String, scalaVersion: String) {

    def colorize(buffer: String)(color: String = "", reset: String = "") = {
      val lines = buffer.split("""\n""")
      val width = lines.max.length
      lines map (r => color + r + Seq.fill(width - r.length)(" ").mkString + reset)
    }

    if (!java.lang.Boolean.getBoolean("sbt.lift.nologo")) {
      import Logo.{Arrow => arr, Text => txt, About}
      val abt = About.copy(_1 = About._1.format(name, version, scalaVersion))

      val colorBuffer =
        Seq(arr, txt, abt) map { l => if (ConsoleAppender.formatEnabledInEnv) colorize(l._1)(l._2, RESET) else colorize(l._1)() }

      val prepBuffer = colorBuffer.reduce((x,y) => x.zipAll(y, "", "").map(_.productIterator.mkString(" ", "   ", "")))
      println(prepBuffer.mkString("", "\n", "\n"))
    }
  }
}

object LiftBuildPlugin extends AutoPlugin with LiftDefaults {
  override lazy val projectSettings = liftDefaultSettings
}
