package com.pagerduty.arrivals.aggregator

import akka.stream.Materializer
import com.pagerduty.arrivals.api.headerauth.HeaderAuthConfig
import com.pagerduty.arrivals.proxy.HttpProxyLike

import scala.concurrent.ExecutionContext

/** Dependencies used by [[Aggregator]] to execute requests, consume response bodies, etc. */
case class AggregatorDependencies[AddressingConfig](
    authConfig: HeaderAuthConfig,
    httpProxy: HttpProxyLike[AddressingConfig],
    executionContext: ExecutionContext,
    materializer: Materializer)
