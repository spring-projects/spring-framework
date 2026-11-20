/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.socket.sockjs.transport.handler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;
import org.springframework.web.socket.sockjs.transport.SockJsServiceConfig;
import org.springframework.web.socket.sockjs.transport.TransportHandler;

/**
 * Common base class for {@link TransportHandler} implementations.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractTransportHandler implements TransportHandler {

	protected final Log logger = LogFactory.getLog(getClass());

	private @Nullable SockJsServiceConfig serviceConfig;


	@Override
	public void initialize(SockJsServiceConfig serviceConfig) {
		this.serviceConfig = serviceConfig;
	}

	public SockJsServiceConfig getServiceConfig() {
		Assert.state(this.serviceConfig != null, "No SockJsServiceConfig available");
		return this.serviceConfig;
	}

}
