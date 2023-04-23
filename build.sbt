ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.1.3"

lazy val root = (project in file("."))
  .settings(
    name := "moodbot2"
  )

libraryDependencies := Seq(
  "org.scalactic" %% "scalactic" % "3.2.15",
  "org.scalatest" %% "scalatest" % "3.2.15" % "test",
  "org.scalikejdbc" %% "scalikejdbc" % "4.0.0",
  "ch.qos.logback"  %  "logback-classic" % "1.4.6",
  "com.github.pengrad" % "java-telegram-bot-api" % "6.7.0"
)