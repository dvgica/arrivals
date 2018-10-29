package com.pagerduty.arrivals.example

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import com.pagerduty.arrivals.impl.ArrivalsServer
import com.pagerduty.metrics.{Metrics, NullMetrics}

object ExampleGateway {
  val CatsUpstream = ExampleUpstream(22000, "cats")
  val DogsUpstream = ExampleUpstream(33000, "dogs")
}

class ExampleGateway(addressingConfig: String,
                     headerAuthConfig: ExampleAuthConfig,
                     listenInterface: String = "0.0.0.0",
                     listenPort: Int = 8080)(implicit actorSystem: ActorSystem,
                                             materializer: Materializer,
                                             metrics: Metrics = NullMetrics)
    extends ArrivalsServer(addressingConfig,
                           headerAuthConfig,
                           listenInterface,
                           listenPort) {

  import ExampleGateway._

  lazy val routes = pathPrefix("api")(
    prefixProxyRoute("cats", CatsUpstream, ExampleResponseFilter) ~
      prefixAuthProxyRoute("dogs", DogsUpstream) ~
      prefixAggregatorRoute("all", ExampleAggregator)
  )

}
