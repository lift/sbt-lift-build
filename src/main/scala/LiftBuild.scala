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

  // Custom Setting keys
  // -------------------
  val inceptionYear = SettingKey[Option[Int]]("inception-year", "Year in which the project started.")
  // Backport from 0.10.2
  lazy val JavaNet2Repository = "java.net Maven2 Repository" at "http://download.java.net/maven/2"
  lazy val description = SettingKey[String]("description", "Project description.")
  lazy val homepage = SettingKey[Option[URL]]("homepage", "Project homepage.")
  lazy val licenses = SettingKey[Seq[(String, URL)]]("licenses", "Project licenses as (name, url) pairs.")
  lazy val organization = SettingKey[String]("organization", "Organization/group ID.")
  lazy val organizationName = SettingKey[String]("organization-name", "Organization full/formal name.")
  lazy val organizationHomepage = SettingKey[Option[URL]]("organization-homepage", "Organization homepage.")
  // End backport

  lazy val liftDefaultSettings: Seq[Setting[_]] = Seq(
    // Backport from 0.10.2
    description <<= description or name.identity,
    homepage in GlobalScope :== None,
    licenses in GlobalScope :== Nil,
    organization <<= organization or normalizedName.identity,
    organizationName in GlobalScope <<= organizationName or organization.identity,
    organizationHomepage in GlobalScope <<= organizationHomepage or homepage.identity,
    // End backport
    name                             ~= formalize,
    inceptionYear in GlobalScope    :== None,
    inceptionYear                   <<= inceptionYear ?? Some(Cal.getInstance.get(Cal.YEAR)),
    scalacOptions in GlobalScope    ++= Seq("-encoding", "UTF-8"),
    // FIXME: Enable after 0.10.2 (See: https://github.com/harrah/xsbt/issues/147)
    // scaladocOptions /*in GlobalScope*/ <++= (name, version) map { (n, v) =>
    //    Seq("-doc-title", n, "-doc-version", v)
    // },
    resolvers in GlobalScope       <++= version { v =>
      if (v endsWith "-SNAPSHOT") Seq(ScalaToolsSnapshots, JavaNet2Repository) else Seq(JavaNet2Repository)
    },
    shellPrompt in GlobalScope       := { state => "sbt:%s> ".format(Project.extract(state).currentProject.id) }
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
    val Version            = "2.4-SNAPSHOT"
    val CrossScalaVersions = Seq("2.9.0-1", "2.9.0", "2.8.1", "2.8.0")

    val License          = ("Apache License, Version 2.0", url("http://www.apache.org/licenses/LICENSE-2.0.txt"))
    val Homepage         = url("http://www.liftweb.net")
    // val Description      = ""
    val OrganizationName = "WorldWide Conferencing, LLC"
    val InceptionYear    = 2006
  }

  // Repositories
  // ------------
  object DistributionRepositories {
    lazy val Local    = Resolver.file("Local Repository", Path.userHome / ".m2" / "repository" asFile)
    lazy val Snapshot = nexusRepo("snapshots")
    lazy val Release  = nexusRepo("releases")

    def nexusRepo(status: String) =
      "Nexus Repository for " + status.capitalize at "http://nexus.scala-tools.org/content/repositories/" + status
  }

  // Credential Sources
  // ------------------
  object CredentialSources {
    lazy val Default = Path.userHome / ".ivy2" / ".scalatools.credentials"
    lazy val Maven   = Path.userHome / ".m2" / "settings.xml"
  }

  import ProjectInfo._
  lazy val liftBuildSettings = liftDefaultSettings ++ Seq(

    organization        := Organization,
    version             := Version,
    crossScalaVersions  := CrossScalaVersions,

    licenses in GlobalScope         += License,
    homepage in GlobalScope         := Some(Homepage),
    organizationName in GlobalScope := OrganizationName,
    inceptionYear in GlobalScope    := Some(InceptionYear),

    scalacOptions in GlobalScope ++= Seq("-unchecked"), // Also should enable "-Xcheckinit", -Xwarninit" when things get in good order

    // In case you want ~/.m2/repository to add to the resolver list
    // Note that you should not do a simple `resolvers += Resolver.mavenLocal`
    // because ~/.m2/repository needs to be looked up ahead of other resolvers.
    // resolvers in GlobalScope ~= { Resolver.mavenLocal +: _ },

    // Also see: https://github.com/indrajitr/xsbt/commit/6ab0f39a5ac5fba06192f32ee988c635612ba4e3#commitcomment-518281
    publishTo in GlobalScope <<= version { v =>
      import DistributionRepositories._
      if (v endsWith "-SNAPSHOT") Some(Snapshot) else Some(Release)
    },

    pomExtra    <<= (pomExtra, inceptionYear) apply toPomExtra,

    credentials <<= (credentials, streams) map addCredentials

  )

  private def toPomExtra(pomExtra: xml.NodeSeq, startYear: Option[Int]) =
    pomExtra ++ startYear map { y => <inceptionYear>{y}</inceptionYear> }

  private def addCredentials(credentials: Seq[Credentials], streams: TaskStreams) = {
    import CredentialSources.{Default => sbt, Maven => mvn}
    import streams.{log => l}
    // FIXME: this isn't optimally idiomatic :(
    (sbt, mvn) match {
      case (s, _) if s.exists => credentials :+ Credentials(s)
      case (_, m) if m.exists => credentials ++ MavenCredentials(m, l)
      case _                  => {
        l.warn("Neither of the files %s or %s available to load server credentials from.".format(sbt, mvn))
        credentials
      }
    }
  }

  object MavenCredentials {
    val nexusRealm = "Sonatype Nexus Repository Manager"

    def apply(file: File, log: Logger): Seq[Credentials] = {
      val tuples = loadMavenCredentials(file) match {
        case Left(err)   => log.warn(err); Nil
        case Right(info) => info
      }
      tuples map { t =>
        // settings.xml doesn't keep auth realm but SBT expects one,
        // do a wild guess about the service by looking at the hostname
        // and blindly default to something that just about works
        if (t._1.contains("nexus")) Credentials(nexusRealm, t._1, t._2, t._3)
        else Credentials("Unknown", t._1, t._2, t._3)
      }
    }

    def loadMavenCredentials(file: File): Either[String, Seq[(String, String, String)]] = {
      if (file.exists) {
        try {
          val creds = xml.XML.loadFile(file) \ "servers" \ "server" map { s =>
            (s \ "id" text, s \ "username" text, s \ "password" text)
          }
          Either.cond(!creds.isEmpty, creds, "No server credential found in Maven settings file " + file)
        } catch {
          case e => Left("Could not read the settings file %s [%s]".format(file, e.getMessage))
        }
      } else Left("Maven settings file " + file + " does not exist")
    }
  }

}
