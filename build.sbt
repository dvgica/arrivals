lazy val akkaVersion = "2.5.9"
lazy val akkaHttpVersion = "10.0.11"
lazy val scalaMetricsVersion = "2.0.0"
lazy val scalaTestVersion = "3.0.4"
lazy val scalaMockVersion = "4.1.0"
lazy val akkaSupportHttpVersion = "0.6.4"
lazy val logbackVersion = "1.2+"

lazy val sharedSettings = Seq(
  organization := "com.pagerduty",
  scalaVersion := "2.12.5",
  licenses += ("Apache-2.0", url(
    "https://www.apache.org/licenses/LICENSE-2.0.html")),
  bintrayOrganization := Some("pagerduty"),
  bintrayRepository := "oss-maven",
  publishMavenStyle := true,
  resolvers := Seq(
    "bintray-pagerduty-oss-maven" at "https://dl.bintray.com/pagerduty/oss-maven",
    Resolver.defaultLocal
  )
)

lazy val akkaHttpRequestAuthentication =
  (project in file("akka-http-request-authentication"))
    .settings(sharedSettings: _*)
    .settings(
      name := "akka-http-request-authentication",
      libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-actor" % akkaVersion,
        "com.typesafe.akka" %% "akka-stream" % akkaVersion,
        "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
        "com.pagerduty" %% "metrics-api" % scalaMetricsVersion,
        "com.pagerduty" %% "akka-support-http" % akkaSupportHttpVersion,
        "org.scalatest" %% "scalatest" % scalaTestVersion % "test",
        "org.scalamock" %% "scalamock" % scalaMockVersion % "test",
        "ch.qos.logback" % "logback-classic" % logbackVersion % "test"
      )
    )

lazy val akkaHttpHeaderAuthentication =
  (project in file("akka-http-header-authentication"))
    .dependsOn(akkaHttpRequestAuthentication)
    .settings(sharedSettings: _*)
    .settings(
      name := "akka-http-header-authentication",
      libraryDependencies ++= Seq(
        "org.scalatest" %% "scalatest" % scalaTestVersion % "test",
        "org.scalamock" %% "scalamock" % scalaMockVersion % "test",
        "ch.qos.logback" % "logback-classic" % logbackVersion % "test"
      )
    )

lazy val akkaHttpProxy = (project in file("akka-http-proxy"))
  .settings(sharedSettings: _*)
  .settings(
    name := "akka-http-proxy",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.pagerduty" %% "metrics-api" % scalaMetricsVersion,
      "com.pagerduty" %% "akka-support-http" % akkaSupportHttpVersion,
      "org.scalatest" %% "scalatest" % scalaTestVersion % "test",
      "org.scalamock" %% "scalamock" % scalaMockVersion % "test",
      "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % "test",
      "ch.qos.logback" % "logback-classic" % logbackVersion % "test"
    )
  )

lazy val akkaHttpAuthProxy = (project in file("akka-http-auth-proxy"))
  .dependsOn(akkaHttpProxy, akkaHttpHeaderAuthentication)
  .configs(IntegrationTest)
  .settings(Defaults.itSettings: _*)
  .settings(inConfig(IntegrationTest)(scalafmtSettings))
  .settings(sharedSettings: _*)
  .settings(
    name := "akka-http-auth-proxy",
    libraryDependencies ++= Seq(
      "org.scalamock" %% "scalamock" % scalaMockVersion % "test",
      "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % "test",
      "org.scalatest" %% "scalatest" % scalaTestVersion % "it,test",
      "ch.qos.logback" % "logback-classic" % logbackVersion % "test,it",
      "org.scalaj" %% "scalaj-http" % "2.3.0" % "it",
      "com.github.tomakehurst" % "wiremock" % "2.8.0" % "it",
      "com.typesafe.akka" %% "akka-slf4j" % "2.4.20" % "it"
    )
  )

lazy val akkaHttpAggregator = (project in file("akka-http-aggregator"))
  .dependsOn(akkaHttpProxy, akkaHttpHeaderAuthentication)
  .settings(sharedSettings: _*)
  .settings(
    name := "akka-http-aggregator",
    libraryDependencies ++= Seq(
      "com.lihaoyi" %% "ujson" % "0.6.6",
      "org.scalamock" %% "scalamock" % scalaMockVersion % "test",
      "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % "test",
      "org.scalatest" %% "scalatest" % scalaTestVersion % "test",
      "ch.qos.logback" % "logback-classic" % logbackVersion % "test"
    )
  )

scalafmtOnCompile in ThisBuild := true
skip in publish := true
