package com.pagerduty.arrivals.impl.aggregator

import akka.stream.Materializer
import com.pagerduty.arrivals.api.headerauth.HeaderAuthConfig
import com.pagerduty.arrivals.impl.proxy.HttpProxy

import scala.concurrent.ExecutionContext

case class AggregatorDependencies[AddressingConfig](
    authConfig: HeaderAuthConfig,
    httpProxy: HttpProxy[AddressingConfig],
    executionContext: ExecutionContext,
    materializer: Materializer)
