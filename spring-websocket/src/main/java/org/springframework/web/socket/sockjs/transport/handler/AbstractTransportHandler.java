/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.socket.sockjs.transport.handler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
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

	@Nullable
	private SockJsServiceConfig serviceConfig;


	@Override
	public void initialize(SockJsServiceConfig serviceConfig) {
		this.serviceConfig = serviceConfig;
	}

	public SockJsServiceConfig getServiceConfig() {
		Assert.state(this.serviceConfig != null, "No SockJsServiceConfig available");
		return this.serviceConfig;
	}

}
