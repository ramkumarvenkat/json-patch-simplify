name := "json-patch-simplify"

version := "0.1"

scalaVersion := "2.13.1"

resolvers += "Maven Repo" at "https://mvnrepository.com/repos/central"

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-json" % "2.8.0-M6",
  "org.specs2" %% "specs2-core" % "4.7.1" % Test
)
