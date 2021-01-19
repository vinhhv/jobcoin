val finchVersion = "0.32.1"
val sttpVersion = "3.0.0"
val catsVersion = "2.3.1"
val circeVersion = "0.13.0"
val catBirdVersion = "20.10.0"
val scalatestVersion = "3.1.1"

lazy val root = (project in file("."))
  .settings(
    organization := "vinhhv.io",
    name := "jobcoin",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.12.7",
    scalacOptions += "-Ypartial-unification",
    libraryDependencies ++= Seq(
      "com.github.finagle" %% "finchx-core"  % finchVersion,
      "com.github.finagle" %% "finchx-circe"  % finchVersion,
      "com.softwaremill.sttp.client3" %% "core" % sttpVersion,
      "com.typesafe" % "config" % "1.4.1",
      "io.catbird" %% "catbird-finagle" % catBirdVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "org.typelevel" %% "cats-core" % catsVersion,
      "org.scalatest" %% "scalatest" % scalatestVersion % "test"
    )
  )