package com.pagerduty.arrivals.impl.aggregator

import akka.NotUsed
import com.pagerduty.arrivals.api

trait OneStepAggregator[AuthData, RequestKey, AddressingConfig]
    extends api.aggregator.OneStepAggregator[AuthData,
                                             RequestKey,
                                             AddressingConfig]
    with Aggregator[AuthData, RequestKey, NotUsed, AddressingConfig]
