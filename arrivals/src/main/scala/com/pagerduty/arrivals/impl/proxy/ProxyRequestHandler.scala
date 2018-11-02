package com.pagerduty.arrivals.impl.proxy

import com.pagerduty.arrivals.api.proxy.Upstream
import com.pagerduty.arrivals.impl.RequestHandler

trait ProxyRequestHandler[AddressingConfig] extends RequestHandler[Upstream[AddressingConfig], Unit]
