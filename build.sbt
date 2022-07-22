name := "CreditLimitChecker"

version := "0.1"

scalaVersion := "2.13.6"

val catsVersion = "2.7.0"
val akkaVersion = "10.2.9"
val akkaStreamVersion = "2.6.18"
val akkaStreamAlpakkaVersion = "3.0.4"

val scalaTagVersion = "0.11.1"
val commonsIOVersion = "2.11.0"

val scalaTestVersion = "3.2.11"

libraryDependencies := Seq(
  "org.typelevel" %% "cats-core" % catsVersion,
  "com.typesafe.akka" %% "akka-http" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaStreamVersion,
  "com.lihaoyi" %% "scalatags" % scalaTagVersion,
  "com.lightbend.akka" %% "akka-stream-alpakka-csv" % akkaStreamAlpakkaVersion,
  "commons-io" % "commons-io" % commonsIOVersion,
  "org.scala-graph" %% "graph-core" % "1.13.4",
  "org.scalatest" %% "scalatest" % scalaTestVersion % Test,
)