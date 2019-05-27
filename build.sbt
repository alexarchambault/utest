import sbtcrossproject.{crossProject, CrossType}
import com.typesafe.sbt.pgp.PgpKeys._
import sbt.Keys.scalacOptions
import sbt.addCompilerPlugin
import sbt.librarymanagement.{SemanticSelector, VersionNumber}

inThisBuild(List(
  organization := "com.lihaoyi"
  homepage := Some(url("https://github.com/lihaoyi/utest")),
  licenses := Seq("MIT" -> url("http://www.opensource.org/licenses/mit-license.html")),
  developers := List(
    Developer(
      email = "haoyi.sg@gmail.com",
      id = "lihaoyi",
      name = "Li Haoyi",
      url = url("https://github.com/lihaoyi")
    )
  )
))

val scala210 = "2.10.7"
val scala211 = "2.11.12"
val scala212 = "2.12.8"
val scala213 = "2.13.0-RC2"

name               in ThisBuild := "utest"
scalaVersion       in ThisBuild := scala212
crossScalaVersions in ThisBuild := Seq(scala210, scala211, scala212, scala213)
updateOptions      in ThisBuild := (updateOptions in ThisBuild).value.withCachedResolution(true)
incOptions         in ThisBuild := (incOptions in ThisBuild).value.withLogRecompileOnMacro(false)
//triggeredMessage   in ThisBuild := Watched.clearWhenTriggered

lazy val utest = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .settings(
    name                  := "utest",
    scalacOptions         := Seq("-Ywarn-dead-code", "-feature"),
    scalacOptions in Test -= "-Ywarn-dead-code",
    libraryDependencies  ++= macroDependencies(scalaVersion.value),
    scalacOptions        ++= (scalaVersion.value match {
      case x if x startsWith "2.13." => "-target:jvm-1.8" :: Nil
      case x if x startsWith "2.12." => "-target:jvm-1.8" :: "-opt:l:method" :: Nil
      case x if x startsWith "2.11." => "-target:jvm-1.6" :: Nil
      case x if x startsWith "2.10." => "-target:jvm-1.6" :: Nil
    }),

    unmanagedSourceDirectories in Compile += {
      val v = "scala-" + scalaVersion.value.split("\\.").take(2).mkString(".")
      baseDirectory.value/".."/"shared"/"src"/"main"/v
    },
    unmanagedSourceDirectories in Compile ++= {
      if (VersionNumber(scalaVersion.value).matchesSemVer(SemanticSelector(s"<$scala213"))) {
        baseDirectory.value/".."/"shared"/"src"/"main"/"scala-pre-2.13" :: Nil
      } else {
        Nil
      }
    },
    testFrameworks += new TestFramework("test.utest.CustomFramework"),

    // Release settings
    publishArtifact in Test := false,
//    autoCompilerPlugins := true,
//
//    addCompilerPlugin("com.lihaoyi" %% "acyclic" % "0.1.7"),
//
//    scalacOptions += "-P:acyclic:force"

)
  .jsSettings(
    libraryDependencies ++= Seq(
      "org.scala-js" %% "scalajs-test-interface" % scalaJSVersion,
      "org.portable-scala" %%% "portable-scala-reflect" % "0.1.0"
    )
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      "org.scala-sbt" % "test-interface" % "1.0",
      "org.portable-scala" %%% "portable-scala-reflect" % "0.1.0"
    ),
    resolvers += Resolver.sonatypeRepo("snapshots")
  )
  .nativeSettings(
    scalaVersion := scala211,
    crossScalaVersions := Seq(scala211),
    libraryDependencies ++= Seq(
      "org.scala-native" %%% "test-interface" % nativeVersion
    ),
    nativeLinkStubs := true
  )

def macroDependencies(version: String) =
  ("org.scala-lang" % "scala-reflect" % version) +:
  (if (version startsWith "2.10.")
     Seq(compilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full),
         "org.scalamacros" %% "quasiquotes" % "2.1.0")
   else
     Seq())

lazy val root = project.in(file("."))
  .aggregate(utest.js, utest.jvm, utest.native)
  .settings(
    skip in publish := true)
