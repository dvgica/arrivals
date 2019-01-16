lazy val akkaVersion = "2.5.19"
lazy val akkaHttpVersion = "10.1.7"
lazy val scalaMetricsVersion = "2.0.0"
lazy val scalaTestVersion = "3.0.4"
lazy val scalaMockVersion = "4.1.0"
lazy val akkaSupportHttpVersion = "0.7.1"
lazy val logbackVersion = "1.2+"

lazy val sharedSettings = Seq(
  organization := "com.pagerduty",
  scalaVersion := "2.12.8",
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
    .settings(inConfig(IntegrationTest)(scalafmtSettings))
    .settings(sharedSettings: _*)
    .settings(
      name := "arrivals",
      libraryDependencies ++= Seq(
        "com.lihaoyi" %% "ujson" % "0.6.6",
        "com.pagerduty" %% "metrics-api" % scalaMetricsVersion,
        "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % "test",
        "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % "test",
        "org.scalamock" %% "scalamock" % scalaMockVersion % "test",
        "org.scalatest" %% "scalatest" % scalaTestVersion % "test,it",
        "ch.qos.logback" % "logback-classic" % logbackVersion % "test,it",
        "org.scalaj" %% "scalaj-http" % "2.3.0" % "it",
        "com.github.tomakehurst" % "wiremock" % "2.8.0" % "it",
        "com.typesafe.akka" %% "akka-slf4j" % "2.4.20" % "it"
      )
    )

lazy val arrivalsExample =
  (project in file("arrivals-example"))
    .dependsOn(arrivals)
    .settings(sharedSettings: _*)
    .settings(
      name := "arrivals-example",
      libraryDependencies ++= Seq(
        "ch.qos.logback" % "logback-classic" % logbackVersion
      )
    )

scalafmtOnCompile in ThisBuild := true
skip in publish := true
