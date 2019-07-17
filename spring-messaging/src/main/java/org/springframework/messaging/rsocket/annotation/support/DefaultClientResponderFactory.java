/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.messaging.rsocket.annotation.support;

import java.util.Arrays;
import java.util.List;

import io.rsocket.RSocketFactory;

import org.springframework.lang.Nullable;
import org.springframework.messaging.handler.invocation.reactive.ArgumentResolverConfigurer;
import org.springframework.messaging.handler.invocation.reactive.ReturnValueHandlerConfigurer;
import org.springframework.messaging.rsocket.MetadataExtractor;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.util.Assert;
import org.springframework.util.RouteMatcher;

/**
 * Default implementation of {@link ClientResponderFactory}.
 *
 * @author Brian Clozel
 */
class DefaultClientResponderFactory implements ClientResponderFactory, ClientResponderFactory.Config {

	@Nullable
	private List<Object> handlers;

	@Nullable
	private RSocketStrategies strategies;

	@Nullable
	private RouteMatcher routeMatcher;

	@Nullable
	private MetadataExtractor extractor;

	@Nullable
	private ReturnValueHandlerConfigurer returnValueHandlerConfigurer;

	@Nullable
	private ArgumentResolverConfigurer argumentResolverConfigurer;


	@Override
	public ClientResponderFactory handlers(Object... handlers) {
		Assert.notEmpty(handlers, "Handlers array must not be empty");
		this.handlers = Arrays.asList(handlers);
		return this;
	}

	@Override
	public ClientResponderFactory.Config strategies(RSocketStrategies strategies) {
		this.strategies = strategies;
		return this;
	}

	@Override
	public ClientResponderFactory.Config routeMatcher(RouteMatcher routeMatcher) {
		this.routeMatcher = routeMatcher;
		return this;
	}

	@Override
	public ClientResponderFactory.Config metadataExtractor(MetadataExtractor extractor) {
		this.extractor = extractor;
		return this;
	}

	@Override
	public ClientResponderFactory.Config returnValueHandler(ReturnValueHandlerConfigurer configurer) {
		this.returnValueHandlerConfigurer = configurer;
		return this;
	}

	@Override
	public ClientResponderFactory.Config argumentResolver(ArgumentResolverConfigurer configurer) {
		this.argumentResolverConfigurer = configurer;
		return this;
	}


	@Override
	public void accept(RSocketFactory.ClientRSocketFactory clientRSocketFactory) {
		Assert.state(this.handlers != null, "No handlers set");
		RSocketMessageHandler messageHandler = new RSocketMessageHandler();
		messageHandler.setHandlers(this.handlers);
		if (this.strategies != null) {
			messageHandler.setRSocketStrategies(this.strategies);
		}
		if (this.routeMatcher != null) {
			messageHandler.setRouteMatcher(this.routeMatcher);
		}
		if (this.extractor != null) {
			messageHandler.setMetadataExtractor(this.extractor);
		}
		if (this.returnValueHandlerConfigurer != null) {
			messageHandler.setReturnValueHandlerConfigurer(this.returnValueHandlerConfigurer);
		}
		if (this.argumentResolverConfigurer != null) {
			messageHandler.setArgumentResolverConfigurer(this.argumentResolverConfigurer);
		}
		messageHandler.afterPropertiesSet();
		clientRSocketFactory.acceptor(messageHandler.clientResponder());
	}

}
