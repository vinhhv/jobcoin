val finchVersion = "0.31.0"
val sttpVersion = "3.0.0"
val circeVersion = "0.13.0"
val catBirdVersion = "20.10.0"
val scalatestVersion = "3.1.1"

lazy val root = (project in file("."))
  .settings(
    organization := "vinhhv.io",
    name := "jobcoin",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.12.7",
    libraryDependencies ++= Seq(
      "com.github.finagle" %% "finchx-core"  % finchVersion,
      "com.github.finagle" %% "finchx-circe"  % finchVersion,
      "com.softwaremill.sttp.client3" %% "core" % sttpVersion,
      "io.catbird" %% "catbird-finagle" % catBirdVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "org.scalatest" %% "scalatest" % scalatestVersion % "test"
    )
  )