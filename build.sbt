//ThisBuild / version := "0.2.0"

//ThisBuild / scalaVersion := "3.2.2"

lazy val root = (project in file("."))
  .settings(
    name := "moodbot",
    version := "0.2",
    scalaVersion := "3.2.2"
  )

libraryDependencies := Seq(
  "org.scalactic" %% "scalactic" % "3.2.15",
  "org.scalatest" %% "scalatest" % "3.2.15" % "test",
  "org.scalikejdbc" %% "scalikejdbc" % "4.0.0",
  "org.postgresql" % "postgresql" % "42.6.0",
  "com.h2database"  %  "h2" % "1.4.200" % "test",
  "ch.qos.logback"  %  "logback-classic" % "1.4.6",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
  "org.jfree" % "jfreechart" % "1.5.4",
  "com.github.pengrad" % "java-telegram-bot-api" % "6.7.0"
)