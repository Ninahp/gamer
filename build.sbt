name := "akkahttp-quickstart"

version := "0.1"
resolvers += "eduardofcbg" at "http://dl.bintray.com/eduardofcbg/maven"

val akkaVersion      = "2.5.19"
val akkaHttpVersion  = "10.1.7"
scalaVersion := "2.12.7"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor"     % akkaVersion,
  "com.typesafe.akka" %% "akka-stream"    % akkaVersion,
  "com.typesafe.akka" %% "akka-http"      % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-xml"  % "10.0.9",
  "com.github.etaty"  %% "rediscala"      % "1.8.0"

)