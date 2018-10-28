package com.pagerduty.arrivals.impl.proxy

import com.pagerduty.arrivals.api.RequestHandler
import com.pagerduty.arrivals.api.proxy.Upstream

trait ProxyRequestHandler[AddressingConfig]
    extends RequestHandler[Upstream[AddressingConfig], Unit]
