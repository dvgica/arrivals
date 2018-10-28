package com.pagerduty.arrivals.impl.aggregator

import com.pagerduty.arrivals.api.RequestHandler
import com.pagerduty.arrivals.api.aggregator.AggregatorDependencies

trait AggregatorRequestHandler[AddressingConfig, AuthData]
    extends RequestHandler[AggregatorDependencies[AddressingConfig], AuthData]
