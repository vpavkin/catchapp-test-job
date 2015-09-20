name := "catchapp-test-job"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.11.7"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

libraryDependencies ++= Seq(
  "org.scalaz" %% "scalaz-core" % "7.1.4",
  "com.softwaremill.macwire" %% "macros" % "1.0.5",
  "com.mandrillapp.wrapper.lutung" % "lutung" % "0.0.5",
  "com.github.nscala-time" %% "nscala-time" % "2.2.0",
  "net.databinder.dispatch" %% "dispatch-core" % "0.11.2"
)
