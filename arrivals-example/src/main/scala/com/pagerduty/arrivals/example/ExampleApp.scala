package com.pagerduty.arrivals.example

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.Directives._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.StdIn

object ExampleApp extends App {
  implicit val system = ActorSystem()
  implicit val ec = system.dispatcher
  implicit val mat = ActorMaterializer()

  // start the cats and dogs upstreams for demo purposes
  val catRoute = get {
    path("api" / "cats") {
      complete {
        (StatusCodes.OK, "Here are some cats: mittens, garfield, tiger")
      }
    }
  }

  val catServer =
    Await.result(Http().bindAndHandle(catRoute, "localhost", 22000), 5.seconds)

  val dogRoute = get {
    path("api" / "dogs") {
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
    Await.result(Http().bindAndHandle(dogRoute, "localhost", 33000), 5.seconds)

  // start the gateway that we'll talk to at 8080
  val gateway = new ExampleGateway("localhost", new ExampleAuthConfig)

  StdIn.readLine() // let it run until user presses return
  gateway.stop()
  dogServer.unbind()
  catServer.unbind()
  system.terminate()
}
