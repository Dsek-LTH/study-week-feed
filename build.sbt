name := "study-week-feed"

version := "0.1"

scalaVersion := "2.12.5"

libraryDependencies += "net.sf.biweekly" % "biweekly" % "0.6.1"
libraryDependencies += "org.tpolecat" % "doobie-core_2.12" % "0.5.2"
libraryDependencies += "mysql" % "mysql-connector-java" % "6.0.6"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % "test"
libraryDependencies += "org.tpolecat" %% "doobie-scalatest" % "0.5.2" % "test"
libraryDependencies += "com.typesafe" % "config" % "1.3.2"