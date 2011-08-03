package net.liftweb.sbt

import scala.xml.{NodeSeq, Text}
import sbt._
import Keys._

object LiftBuildPlugin extends Plugin {
  
  val projectOrganization       = "net.liftweb"
  val projectVersion            = "2.4-SNAPSHOT"
  // val projectScalaVersion       = "2.8.1"
  val projectScalaVersion       = "2.9.0-1"
  val projectCrossScalaVersions = Seq("2.8.0", "2.8.1", "2.9.0", "2.9.0-1")
  
  val projectLicense          = ("Apache License, Version 2.0", url("http://www.apache.org/licenses/LICENSE-2.0.txt"))
  val projectHomepage         = url("http://www.liftweb.net")
  val projectDescription      = ""
  val projectOrganizationName = "WorldWide Conferencing, LLC"

  // Repositories
  // ------------
  object DistributionRepositories {
    lazy val Local    = Resolver.file("Local Repository", Path.userHome / ".m2" / "repository" asFile)
    lazy val Snapshot = nexusRepo("snapshots")
    lazy val Release  = nexusRepo("releases")

    def nexusRepo(status: String) =
      "Nexus Repository for " + status.capitalize at "http://nexus.scala-tools.org/content/repositories/" + status
  }


  lazy val liftBuildSettings = Seq(

    // Library coordinates
    name         ~= formalize,
    organization := projectOrganization,
    version      := projectVersion,
    scalaVersion := projectScalaVersion,
    // crossScalaVersions := projectCrossScalaVersions,

////////
    // projectID <<= projectID { _.extra("e:color" -> "red") },
////////

    description ~= { d =>
      if (!projectDescription.isEmpty) else d 
    },
    homepage    := Some(projectHomepage),
    licenses    += projectLicense,
    // description <<= description or name.identity, // Up there
    // description <<= name apply idFun, // Up there

    organizationName := Some(projectOrganizationName),

    scalacOptions ++= Seq("-encoding", "UTF-8", /*"-deprecation", */"-unchecked"),

    resolvers <++= version { v =>
      /*Resolver.mavenLocal ::*/ JavaNet2Repository :: (if (v endsWith "-SNAPSHOT") ScalaToolsSnapshots :: Nil else Nil)
    },

    publishTo <<= version { v =>
      import DistributionRepositories._
      if (v endsWith "-SNAPSHOT") Some(Snapshot) else Some(Release)
    }

    // pomExtra <<= (pomExtra, name, organizationName, organizationHomepage) apply toPomExtra

    // ivyXML <<= (projectID, licenses, description, homepage, scalaVersion) apply toInfoXml
  )

  def formalize(name: String) = name.split("-") map(_.capitalize) mkString(" ")

  private def toPomExtra(
      pomExtra: NodeSeq,
      name: String,
      orgName: String,
      orgHomepage: Option[URL]) = {
    pomExtra ++
    <name>{name}</name>
    <organization>
      <name>{orgName}</name>
      { orgHomepage map { h => <url>{h}</url> } getOrElse NodeSeq.Empty }
    </organization>
  }

  private def toInfoXml(
      project:      ModuleID,
      licenses:     Seq[(String, URL)], 
      description:  String,
      homepage:     Option[URL],
      scalaVersion: String) = {

    val moduleAttr = if(project.crossVersion) project.name + "_" + scalaVersion else project.name

    def makeLicensesXml(licenses: Seq[(String, URL)]) =
      licenses map { l => <license name={l._1} url={l._2.toString} /> }

    def makeDescriptionXml(description: String, homepage: Option[URL]) =
      <description homepage={homepage map (h => Text(h.toString))}>{description}</description>

    // def makeDescriptionXml(description: Option[String], homepage: Option[URL]) =
    //   (for {
    //     d <- description if !d.trim.isEmpty
    //     dxml = <description homepage={homepage map (h => Text(h.toString))}>{d}</description>
    //   } yield dxml) getOrElse NodeSeq.Empty

    <info organisation={project.organization} module={moduleAttr} revision={project.revision}>
      { makeLicensesXml(licenses) }
      { makeDescriptionXml(description, homepage) }
    </info>
  }

  // See: https://github.com/harrah/xsbt/wiki/Global-Settings
  override def settings = Seq(
    shellPrompt := { state =>
      "sbt (%s)> ".format(Project.extract(state).currentProject.id)
    }
  )

}
