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

import scala.Console.{BLUE, CYAN, GREEN, RESET}
import sbt.*
import internal.util.ConsoleAppender
import Keys.*

import scala.annotation.nowarn

object LiftBuildPlugin extends AutoPlugin {
  object autoImport {

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

    lazy val baseSettings: Seq[Setting[?]] = Seq(
      name           ~= formalize,
      javacOptions  ++= DefaultOptions.javac,
      scalacOptions ++= DefaultOptions.scalac :+ Opts.compile.deprecation, // :+ Opts.compile.unchecked, // FIXME: breaks tests
      packageOptions += Package.ManifestAttributes("Built-By"   -> System.getProperty("user.name", "unknown"),
                                                   "Built-Time" -> java.util.Calendar.getInstance.getTimeInMillis.toString),
      DefaultOptions.addResolvers,
      DefaultOptions.setupShellPrompt
    )

    lazy val compileSettings: Seq[Setting[?]] = inTask(compile)(Seq(
      javacOptions  += Opts.compile.deprecation
      //scalacOptions += "-Xcheckinit"  // FIXME: breaks lift-util
    ))

    lazy val docSettings: Seq[Setting[?]] = inTask(doc)(Seq(
      javacOptions  ++= DefaultOptions.javadoc(name.value, version.value),
      scalacOptions ++= DefaultOptions.scaladoc(name.value, version.value)
    ))

    lazy val liftDefaultSettings: Seq[Setting[?]] =
      baseSettings ++ Seq(Compile, Test, IntegrationTest).flatMap(inConfig(_)(compileSettings ++ docSettings))

    def formalize(name: String): String = name.split("-") map (_.capitalize) mkString (" ")

    def defaultOrMapped(defaultValue: String, alternatives: (String, String)*): String => String =
      Map(alternatives*) orElse { case _ => defaultValue }

    // Logo printer
    def printLogo(name: String, version: String, scalaVersion: String): Unit = {

      def colorize(buffer: String)(color: String = "", reset: String = "") = {
        val lines = buffer.split("""\n""")
        val width = lines.max.length
        lines map (r => color + r + Seq.fill(width - r.length)(" ").mkString + reset)
      }

      if (!java.lang.Boolean.getBoolean("sbt.lift.nologo")) {
        import Logo.{Arrow as arr, Text as txt, About}

        val abt = About.copy(_1 = About._1.format(name, version, scalaVersion))

        @nowarn
        val isAnsiSupported = ConsoleAppender.formatEnabledInEnv

        val colorBuffer =
          Seq(arr, txt, abt) map { l => if (isAnsiSupported) colorize(l._1)(l._2, RESET) else colorize(l._1)() }

        val prepBuffer = colorBuffer.reduce((x,y) => x.zipAll(y, "", "").map(_.productIterator.mkString(" ", "   ", "")))
        println(prepBuffer.mkString("", "\n", "\n"))
      }
    }
  }

  override lazy val projectSettings: Seq[Def.Setting[?]] = autoImport.liftDefaultSettings
}
