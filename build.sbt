//ThisBuild / version := "0.2.0"

//ThisBuild / scalaVersion := "3.2.2"

lazy val root = (project in file("."))
  .settings(
    name := "moodbot",
    version := "0.2",
    scalaVersion := "3.2.2"
  )
  .configs(IntegrationTest)
  .settings(Defaults.itSettings)

val testcontainersScalaVersion = "0.40.12"

libraryDependencies := Seq(
  "org.scalactic" %% "scalactic" % "3.2.16" % "test,it",
  "org.scalatest" %% "scalatest" % "3.2.15" % "test,it",
  "org.mockito" % "mockito-core" % "5.2.0",
  "org.scalikejdbc" %% "scalikejdbc" % "4.0.0",
  "org.postgresql" % "postgresql" % "42.6.0",
  "ch.qos.logback"  %  "logback-classic" % "1.4.6",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
  "org.jfree" % "jfreechart" % "1.5.4",
  "com.github.pengrad" % "java-telegram-bot-api" % "6.7.0",
  "com.dimafeng" %% "testcontainers-scala-scalatest" % testcontainersScalaVersion % "it",
  "com.dimafeng" %% "testcontainers-scala-postgresql" % testcontainersScalaVersion % "it"
)

IntegrationTest / fork := true

assembly / assemblyMergeStrategy := {
  case PathList("module-info.class") => MergeStrategy.last
  case path if path.endsWith("/module-info.class") => MergeStrategy.last
  case x =>
    val oldStrategy = (assembly / assemblyMergeStrategy).value
    oldStrategy(x)
}
