package com.pagerduty.arrivals.api.proxy

import com.pagerduty.arrivals.api.RequestResponder

trait HttpProxy[AddressingConfig] extends RequestResponder[Upstream[AddressingConfig], Any]
