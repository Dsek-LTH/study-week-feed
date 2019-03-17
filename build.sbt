name := "study-week-feed"
organization := "dsek"

version := "0.1"

scalaVersion := "2.12.7"

enablePlugins(JavaAppPackaging, AshScriptPlugin)
dockerBaseImage := "openjdk:8-jre-alpine"
packageName in Docker := s"${organization.value}/${name.value}"

libraryDependencies += "net.sf.biweekly" % "biweekly" % "0.6.1"
libraryDependencies += "org.tpolecat" %% "doobie-core" % "0.6.0"
libraryDependencies += "mysql" % "mysql-connector-java" % "8.0.12"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % "test"
libraryDependencies += "org.tpolecat" %% "doobie-scalatest" % "0.6.0" % "test"
libraryDependencies += "com.typesafe" % "config" % "1.3.2"
val http4sVersion = "0.20.0-M6"
libraryDependencies += "org.http4s" %% "http4s-dsl" % http4sVersion
libraryDependencies += "org.http4s" %% "http4s-blaze-server" % http4sVersion
libraryDependencies += "co.fs2" %% "fs2-io" % "1.0.0"
libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.7.26"

scalacOptions ++= Seq("-Ypartial-unification")
