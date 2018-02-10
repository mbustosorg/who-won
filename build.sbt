import com.typesafe.sbt.SbtNativePackager._

packageArchetype.java_application

lazy val commonSettings = Seq(
   organization := "org.bustos",
   version := "0.1.0",
   scalaVersion := "2.11.7"
)

lazy val mainProject = (project in file("."))
    .settings(name := "who-won")
    .settings(commonSettings: _*)
    .settings(libraryDependencies ++= projectLibraries)
    .settings(resolvers += "Spray" at "http://repo.spray.io")

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

val slf4jV = "1.7.6"
val sprayV = "1.3.2"
val akkaV = "2.3.6"

val projectLibraries = Seq(
    "com.google.cloud"        %  "google-cloud-vision" % "1.12.0",
    "com.amazonaws"           %  "aws-java-sdk"    % "1.9.6",
    "io.spray"                %% "spray-can"       % sprayV,
    "io.spray"                %% "spray-routing"   % sprayV,
    "io.spray"                %% "spray-testkit"   % sprayV  % "test",
    "io.spray"                %% "spray-json"      % "1.3.1",
    "com.typesafe.akka"       %% "akka-actor"      % akkaV,
    "com.typesafe.akka"       %% "akka-testkit"    % akkaV   % "test",
    "com.typesafe.slick"      %% "slick"           % "2.1.0",
    "org.seleniumhq.selenium" %  "selenium-java"   % "2.35.0",
    "org.scalatest"           %% "scalatest"       % "2.1.6",
    "org.specs2"              %% "specs2-core"     % "2.3.11" % "test",
    "com.wandoulabs.akka"     %% "spray-websocket" % "0.1.3",
    "com.gettyimages"         %% "spray-swagger"   % "0.5.0",
    "log4j"                   %  "log4j"           % "1.2.14",
    "org.slf4j"               %  "slf4j-api"       % slf4jV,
    "org.slf4j"               %  "slf4j-simple"    % slf4jV,
    "mysql"                   %  "mysql-connector-java" % "5.1.39",
    "joda-time"               %  "joda-time"       % "2.7",
    "org.joda"                %  "joda-convert"    % "1.2",
    "com.github.tototoshi"    %% "scala-csv"       % "1.2.2"
)

Revolver.settings