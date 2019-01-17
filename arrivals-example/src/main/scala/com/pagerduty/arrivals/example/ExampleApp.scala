package com.pagerduty.arrivals.example

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.Directives._
import com.pagerduty.arrivals.ArrivalsContext
import com.pagerduty.arrivals.aggregator.AggregatorRoutes
import com.pagerduty.arrivals.authproxy.AuthProxyRoutes

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.StdIn

object ExampleApp extends App {
  implicit val system = ActorSystem()
  implicit val ec = system.dispatcher
  implicit val mat = ActorMaterializer()

  import ExampleUpstream._

  // start the cats and dogs upstreams for demo purposes
  val catRoute = get {
    path("cats") {
      complete {
        (StatusCodes.OK, "Here are some cats: mittens, garfield, tiger")
      }
    }
  }

  val catServer =
    Await.result(Http().bindAndHandle(catRoute, "localhost", CatsUpstream.port), 5.seconds)

  val dogRoute = get {
    path("dogs") {
      optionalHeaderValueByName("X-User-Id") { userId =>
        complete {
          userId match {
            case Some("2") =>
              (StatusCodes.OK, "Hi Rex! Here are some dogs: rex, king, duke")
            case _ =>
              (StatusCodes.Forbidden, "You're not an authenticated dog. Hint: try adding `?username=rex` to the URL.")
          }
        }
      }
    }
  }

  val dogServer =
    Await.result(Http().bindAndHandle(dogRoute, "localhost", DogsUpstream.port), 5.seconds)

  implicit val arrivalsCtx = ArrivalsContext("localhost")
  implicit val authConfig = new ExampleAuthConfig

  val authProxyDirectives = new AuthProxyRoutes(authConfig)
  val aggregatorDirectives = new AggregatorRoutes(authConfig)

  import com.pagerduty.arrivals.proxy.ProxyRoutes._
  import authProxyDirectives._
  import aggregatorDirectives._

  val ExampleComposedResponseFilter = ExampleResponseFilterOne ~> ExampleResponseFilterTwo

  val apiGatewayRoutes =
    prefixProxyRoute("cats", CatsUpstream, ExampleComposedResponseFilter) ~
      prefixAuthProxyRoute("dogs", DogsUpstream) ~
      prefixAggregatorRoute("all", ExampleAggregator)

  // start the API gateway with Arrivals routes
  val apiGateway =
    Await.result(Http().bindAndHandle(apiGatewayRoutes, "0.0.0.0", 8080), 5.seconds)

  println("Example API Gateway running at localhost:8080")
  println("Press ENTER to exit")

  StdIn.readLine() // let it run until user presses return
  apiGateway.unbind()
  dogServer.unbind()
  catServer.unbind()
  arrivalsCtx.shutdown()
  system.terminate()
}
