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

import java.io.File
import java.util.{Calendar => Cal}
import sbt._
import Keys._

sealed trait LiftDefaults {
  
  lazy val encodingOptions = Seq("-encoding", "UTF-8")

  lazy val liftDefaultSettings: Seq[Setting[_]] = Seq(
    name                          ~= formalize,
    // resolvers in GlobalScope      ~= (Resolver.mavenLocal +: _),
    resolvers in GlobalScope      += JavaNet2Repository,
    resolvers                   <++= isSnapshot { s => if (s) Seq(ScalaToolsSnapshots) else Nil },

    javacOptions in GlobalScope  ++= encodingOptions,
    scalacOptions in GlobalScope ++= encodingOptions,
    scaladocOptions             <++= (name, version) map(Seq("-doc-title", _, "-doc-version", _) ++ encodingOptions),

    shellPrompt in GlobalScope    := { state => "sbt:%s> ".format(Project.extract(state).currentProject.id) }
  )

  def formalize(name: String) = name.split("-") map(_.capitalize) mkString(" ")

  def selectDynamic(default: String, alternatives: (String, String)*)(scalaVersion: String) =
    Map(alternatives: _*).getOrElse(scalaVersion, default)

}

object LiftAppPlugin extends Plugin with LiftDefaults {

  lazy val liftAppSettings = liftDefaultSettings ++ Seq(

  )
}

object LiftBuildPlugin extends Plugin with LiftDefaults {

  // Project Information
  // -------------------
  object ProjectInfo {
    val Organization       = "net.liftweb"
    // Fixme, this should go in respective projects (framework, modules etc.)
    val Version            = "2.4-SNAPSHOT"
    // Fixme, this should go in respective projects (framework, modules etc.)
    val CrossScalaVersions = Seq("2.9.0-1", "2.9.0", "2.8.1", "2.8.0")

    val License          = ("Apache License, Version 2.0", url("http://www.apache.org/licenses/LICENSE-2.0.txt"))
    val Homepage         = url("http://www.liftweb.net")
    // val Description      = ""
    val OrganizationName = "WorldWide Conferencing, LLC"
    val StartYear        = 2006
  }

  import ProjectInfo._
  lazy val liftBuildSettings = liftDefaultSettings ++ Seq(

    organization        := Organization,
    version             := Version,
    crossScalaVersions  := CrossScalaVersions,

    licenses in ThisBuild         += License,
    homepage in ThisBuild         := Some(Homepage),
    organizationName in ThisBuild := OrganizationName,
    startYear in ThisBuild        := Some(StartYear),

    scalacOptions in ThisBuild ++= Seq("-unchecked") // Also should enable "-Xcheckinit", -Xwarninit" when things get in good order
  )
}
