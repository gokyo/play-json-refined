import java.time.LocalDate

import scala.sys.process._
import ReleaseTransformations._

// Do-it-all command for Travis CI
val validateCommands = List(
  "clean",
  "headerCheck",
  "test:headerCheck",
  "compile",
  "test:compile",
  "scalafmt::test",
  "test:scalafmt::test",
  "sbt:scalafmt::test",
  "test"
)
addCommandAlias("validate", validateCommands.mkString(";", ";", ""))

// Additional release step to update our README
lazy val updateVersionInReadme: ReleaseStep = { st: State =>
  val newVersion = Project.extract(st).get(version)

  val pattern = "\"com.lunaryorn\" %% \"play-json-refined\" % \"([^\"]+)\"".r
  val readme = file("README.md")
  val content = IO.read(readme)
  pattern.findFirstMatchIn(content) match {
    case Some(m) => IO.write(readme, m.before(1) + newVersion + m.after(1))
    case None =>
      throw new IllegalStateException("Failed to find version in README")
  }

  Seq("git", "add", readme.getAbsolutePath) !! st.log

  st
}

lazy val updateChangelog: ReleaseStep = { st: State =>
  val newVersion = Project.extract(st).get(version)

  val pattern = "## \\[?Unreleased\\]?".r
  val changelog = file("CHANGELOG.md")
  val content = IO.read(changelog)

  pattern.findFirstMatchIn(content) match {
    case Some(m) =>
      IO.write(
        changelog,
        m.before(0) + m.matched + s"\n\n## $newVersion – ${LocalDate.now().toString}" + m
          .after(0)
      )
    case None =>
      throw new IllegalStateException(
        "Failed to find Unreleased section in CHANGELOG")
  }

  Seq("git", "add", changelog.getAbsolutePath) !! st.log

  st
}

lazy val root = (project in file("."))
  .settings(
    // Build metadata for this project
    name := "play-json-refined",
    organization := "com.lunaryorn",
    organizationName := "Sebastian Wiesner",
    homepage := Some(url("https://github.com/lunaryorn/play-json-refined")),
    licenses += "Apache-2.0" -> url(
      "http://www.apache.org/licenses/LICENSE-2.0"),
    developers := List(
      Developer(
        id = "lunaryorn",
        name = "Sebastian Wiesner",
        email = "sebastian@swsnr.de",
        url = url("https://swsnr.de")
      )
    ),
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/lunaryorn/play-json-refined"),
        "scm:git:https://github.com/lunaryorn/play-json-refined.git",
        Some(s"scm:git:git@github.com:lunaryorn/play-json-refined.git")
      )),
    description := "Play JSON Reads/Writes for refined types",
    startYear := Some(2016),
    // Publish signed artifacts to Maven Central
    publishMavenStyle := true,
    publishTo := Some(sonatypeDefaultResolver.value),
    // Credentials for Travis CI, see
    // http://www.cakesolutions.net/teamblogs/publishing-artefacts-to-oss-sonatype-nexus-using-sbt-and-travis-ci
    credentials ++= (for {
      username <- Option(System.getenv().get("SONATYPE_USERNAME"))
      password <- Option(System.getenv().get("SONATYPE_PASSWORD"))
    } yield
      Credentials("Sonatype Nexus Repository Manager",
                  "oss.sonatype.org",
                  username,
                  password)).toSeq,
    // Release settings
    releasePublishArtifactsAction := PgpKeys.publishSigned.value,
    releaseCrossBuild := true,
    releaseTagComment := s"play-json-refined ${version.value}",
    releaseCommitMessage := s"Bump version to ${version.value}",
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runTest,
      setReleaseVersion,
      updateVersionInReadme,
      updateChangelog,
      commitReleaseVersion,
      tagRelease,
      publishArtifacts,
      setNextVersion,
      commitNextVersion,
      pushChanges,
      releaseStepCommand("sonatypeRelease")
    ),
    // Formatting
    scalafmtVersion := "1.4.0",
    // Dependencies
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play-json" % "2.6.9",
      "eu.timepit" %% "refined" % "0.8.7",
      "org.scalacheck" %% "scalacheck" % "1.13.5" % "test"
    ),
    // Compiler flags.  The scala version comes from sbt-travisci
    scalacOptions ++= Seq(
      // Code encoding
      "-encoding",
      "UTF-8",
      // Deprecation warnings
      "-deprecation",
      // Warnings about features that should be imported explicitly
      "-feature",
      // Enable additional warnings about assumptions in the generated code
      "-unchecked",
      // Recommended additional warnings
      "-Xlint",
      // Warn when argument list is modified to match receiver
      "-Ywarn-adapted-args",
      // Warn about dead code
      "-Ywarn-dead-code",
      // Warn about inaccessible types in signatures
      "-Ywarn-inaccessible",
      // Warn when non-nullary overrides a nullary (def foo() over def foo)
      "-Ywarn-nullary-override",
      // Warn when numerics are unintentionally widened
      "-Ywarn-numeric-widen",
      // Fail compilation on warnings
      "-Xfatal-warnings"
    )
  )
  .enablePlugins(AutomateHeaderPlugin)
