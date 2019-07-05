lazy val scala213 = "2.13.0"
lazy val scala212 = "2.12.8"
lazy val supportedScalaVersions = List(scala213, scala212)

ThisBuild / organization := "com.pagerduty"
ThisBuild / scalaVersion := scala213

lazy val akkaVersion = "2.5.23"
lazy val akkaHttpVersion = "10.1.8"
lazy val scalaMetricsVersion = "2.1.4"
lazy val scalaTestVersion = "3.0.8"
lazy val scalaMockVersion = "4.3.0"
lazy val akkaSupportHttpVersion = "0.7.2"
lazy val logbackVersion = "1.2+"

lazy val sharedSettings = Seq(
  crossScalaVersions := supportedScalaVersions,
  licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html")),
  bintrayOrganization := Some("pagerduty"),
  bintrayRepository := "oss-maven",
  publishMavenStyle := true,
  resolvers := Seq(
    "bintray-pagerduty-oss-maven" at "https://dl.bintray.com/pagerduty/oss-maven",
    Resolver.defaultLocal
  )
)

lazy val arrivalsApi =
  (project in file("arrivals-api"))
    .settings(sharedSettings: _*)
    .settings(
      name := "arrivals-api",
      libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-actor" % akkaVersion,
        "com.typesafe.akka" %% "akka-stream" % akkaVersion,
        "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
        "com.pagerduty" %% "akka-support-http" % akkaSupportHttpVersion
      )
    )

lazy val arrivals =
  (project in file("arrivals"))
    .dependsOn(arrivalsApi)
    .configs(IntegrationTest)
    .settings(Defaults.itSettings: _*)
    .settings(inConfig(IntegrationTest)(org.scalafmt.sbt.ScalafmtPlugin.scalafmtConfigSettings))
    .settings(sharedSettings: _*)
    .settings(
      name := "arrivals",
      libraryDependencies ++= Seq(
        "com.lihaoyi" %% "ujson" % "0.7.5",
        "com.pagerduty" %% "metrics-api" % scalaMetricsVersion,
        "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % "test",
        "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % "test",
        "org.scalamock" %% "scalamock" % scalaMockVersion % "test",
        "org.scalatest" %% "scalatest" % scalaTestVersion % "test,it",
        "ch.qos.logback" % "logback-classic" % logbackVersion % "test,it",
        "org.scalaj" %% "scalaj-http" % "2.4.2" % "it",
        "com.github.tomakehurst" % "wiremock" % "2.22.0" % "it",
        "com.typesafe.akka" %% "akka-slf4j" % akkaVersion % "it"
      )
    )

lazy val arrivalsExample =
  (project in file("arrivals-example"))
    .dependsOn(arrivals)
    .settings(sharedSettings: _*)
    .settings(
      name := "arrivals-example",
      publish / skip := true,
      libraryDependencies ++= Seq(
        "ch.qos.logback" % "logback-classic" % logbackVersion
      )
    )

lazy val root =
  (project in file("."))
    .settings(
      // crossScalaVersions must be set to Nil on the aggregating project
      crossScalaVersions := Nil,
      publish / skip := true,
      mappings in makeSite ++= Seq(
        file("src/site/.circleci/config.yml") -> ".circleci/config.yml"
      ),
      siteSubdirName in ScalaUnidoc := "api",
      addMappingsToSiteDir(mappings in (ScalaUnidoc, packageDoc), siteSubdirName in ScalaUnidoc)
    )
    .enablePlugins(ScalaUnidocPlugin)
    .aggregate(arrivals, arrivalsApi)

scalafmtOnCompile in ThisBuild := true

enablePlugins(GhpagesPlugin)

git.remoteRepo := "git@github.com:PagerDuty/arrivals.git"
