package com.pagerduty.arrivals.impl.authproxy

import com.pagerduty.arrivals.api.RequestHandler
import com.pagerduty.arrivals.api.proxy.Upstream

trait AuthProxyRequestHandler[AddressingConfig, AuthData]
    extends RequestHandler[Upstream[AddressingConfig], Option[AuthData]]
