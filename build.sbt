import com.typesafe.sbt.SbtNativePackager._

packageArchetype.java_application

lazy val commonSettings = Seq(
   organization := "org.bustos",
   version := "0.3.0",
   scalaVersion := "2.12.4"
)

lazy val mainProject = (project in file("."))
    .settings(name := "who-won")
    .settings(commonSettings: _*)
    .settings(libraryDependencies ++= projectLibraries)
    .settings(resolvers += "Spray" at "http://repo.spray.io")

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

val slf4j_version = "1.7.6"
val akka_http_version = "10.0.11"
val akka_version = "2.4.20"

val projectLibraries = Seq(
    "com.google.cloud"        %  "google-cloud-vision"  % "1.12.0",
    "com.amazonaws"           %  "aws-java-sdk"         % "1.9.6",
    "com.typesafe.akka"       %% "akka-http-core"       % akka_http_version,
    "com.typesafe.akka"       %% "akka-http"            % akka_http_version,
    "com.typesafe.akka"       %% "akka-http-spray-json" % akka_http_version,
    "com.typesafe.akka"       %% "akka-actor"           % akka_version,
    "com.typesafe.slick"      %% "slick"                % "2.1.0",
    "com.typesafe.akka"       %% "akka-http-testkit"    % akka_http_version,
    "org.scalatest"           %% "scalatest"            % "3.0.1" % Test,
    "log4j"                   %  "log4j"                % "1.2.14",
    "org.slf4j"               %  "slf4j-api"            % slf4j_version,
    "org.slf4j"               %  "slf4j-simple"         % slf4j_version,
    "mysql"                   %  "mysql-connector-java" % "5.1.39",
    "joda-time"               %  "joda-time"            % "2.7",
    "org.joda"                %  "joda-convert"         % "1.2",
    "com.github.tototoshi"    %% "scala-csv"            % "1.3.5"
)

Revolver.settings