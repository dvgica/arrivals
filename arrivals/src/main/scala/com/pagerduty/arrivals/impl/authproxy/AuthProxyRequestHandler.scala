package com.pagerduty.arrivals.impl.authproxy

import com.pagerduty.arrivals.api.proxy.Upstream
import com.pagerduty.arrivals.impl.RequestHandler

trait AuthProxyRequestHandler[AddressingConfig, AuthData]
    extends RequestHandler[Upstream[AddressingConfig], Option[AuthData]]
