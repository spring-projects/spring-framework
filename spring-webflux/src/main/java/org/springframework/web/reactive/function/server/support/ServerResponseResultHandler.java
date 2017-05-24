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

package org.springframework.web.reactive.function.server.support;

import java.util.Collections;
import java.util.List;

import reactor.core.publisher.Mono;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.Ordered;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.util.Assert;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.HandlerResultHandler;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ServerWebExchange;

/**
 * {@code HandlerResultHandler} implementation that supports {@link ServerResponse}s.
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
public class ServerResponseResultHandler implements HandlerResultHandler, InitializingBean,
		Ordered {

	private ServerCodecConfigurer messageCodecConfigurer;

	private List<ViewResolver> viewResolvers;

	private int order = LOWEST_PRECEDENCE;


	/**
	 * Configure HTTP message readers to de-serialize the request body with.
	 * <p>By default this is set to {@link ServerCodecConfigurer} with defaults.
	 */
	public void setMessageCodecConfigurer(ServerCodecConfigurer configurer) {
		this.messageCodecConfigurer = configurer;
	}

	public void setViewResolvers(List<ViewResolver> viewResolvers) {
		this.viewResolvers = viewResolvers;
	}

	/**
	 * Set the order for this result handler relative to others.
	 * <p>By default set to {@link Ordered#LOWEST_PRECEDENCE}, however see
	 * Javadoc of sub-classes which may change this default.
	 * @param order the order
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		if (this.messageCodecConfigurer == null) {
			throw new IllegalArgumentException("'messageCodecConfigurer' is required");
		}
		if (this.viewResolvers == null) {
			this.viewResolvers = Collections.emptyList();
		}
	}

	@Override
	public boolean supports(HandlerResult result) {
		return (result.getReturnValue() instanceof ServerResponse);
	}

	@Override
	public Mono<Void> handleResult(ServerWebExchange exchange, HandlerResult result) {
		ServerResponse response = (ServerResponse) result.getReturnValue();
		Assert.state(response != null, "No ServerResponse");
		return response.writeTo(exchange, new ServerResponse.Context() {
			@Override
			public List<HttpMessageWriter<?>> messageWriters() {
				return messageCodecConfigurer.getWriters();
			}

			@Override
			public List<ViewResolver> viewResolvers() {
				return viewResolvers;
			}
		});
	}
}
