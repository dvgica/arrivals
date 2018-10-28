package com.pagerduty.arrivals.impl.aggregator

import com.pagerduty.arrivals.api.aggregator.AggregatorDependencies
import com.pagerduty.arrivals.impl.RequestHandler

trait AggregatorRequestHandler[AddressingConfig, AuthData]
    extends RequestHandler[AggregatorDependencies[AddressingConfig], AuthData]
