/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.reactive.function.server.support;

import java.util.Collections;
import java.util.List;

import reactor.core.publisher.Mono;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.handler.AbstractHandlerMapping;
import org.springframework.web.server.ServerWebExchange;

/**
 * {@code HandlerMapping} implementation that supports {@link RouterFunction}s.
 * <p>If no {@link RouterFunction} is provided at
 * {@linkplain #RouterFunctionMapping(RouterFunction) construction time}, this mapping will detect
 * all router functions in the application context, and consult them in
 * {@linkplain org.springframework.core.annotation.Order order}.
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
public class RouterFunctionMapping extends AbstractHandlerMapping implements InitializingBean {

	@Nullable
	private RouterFunction<?> routerFunction;

	private List<HttpMessageReader<?>> messageReaders = Collections.emptyList();


	/**
	 * Create an empty {@code RouterFunctionMapping}.
	 * <p>If this constructor is used, this mapping will detect all {@link RouterFunction} instances
	 * available in the application context.
	 */
	public RouterFunctionMapping() {
	}

	/**
	 * Create a {@code RouterFunctionMapping} with the given {@link RouterFunction}.
	 * <p>If this constructor is used, no application context detection will occur.
	 * @param routerFunction the router function to use for mapping
	 */
	public RouterFunctionMapping(RouterFunction<?> routerFunction) {
		this.routerFunction = routerFunction;
	}


	/**
	 * Return the configured {@link RouterFunction}.
	 * <p><strong>Note:</strong> When router functions are detected from the
	 * ApplicationContext, this method may return {@code null} if invoked
	 * prior to {@link #afterPropertiesSet()}.
	 * @return the router function or {@code null}
	 */
	@Nullable
	public RouterFunction<?> getRouterFunction() {
		return this.routerFunction;
	}

	/**
	 * Configure HTTP message readers to de-serialize the request body with.
	 * <p>By default this is set to the {@link ServerCodecConfigurer}'s defaults.
	 */
	public void setMessageReaders(List<HttpMessageReader<?>> messageReaders) {
		this.messageReaders = messageReaders;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (CollectionUtils.isEmpty(this.messageReaders)) {
			ServerCodecConfigurer codecConfigurer = ServerCodecConfigurer.create();
			this.messageReaders = codecConfigurer.getReaders();
		}

		if (this.routerFunction == null) {
			initRouterFunctions();
		}
	}

	/**
	 * Initialized the router functions by detecting them in the application context.
	 */
	protected void initRouterFunctions() {
		if (logger.isDebugEnabled()) {
			logger.debug("Looking for router functions in application context: " +
					getApplicationContext());
		}

		List<RouterFunction<?>> routerFunctions = routerFunctions();
		if (!CollectionUtils.isEmpty(routerFunctions) && logger.isInfoEnabled()) {
			routerFunctions.forEach(routerFunction -> logger.info("Mapped " + routerFunction));
		}
		this.routerFunction = routerFunctions.stream()
				.reduce(RouterFunction::andOther)
				.orElse(null);
	}

	private List<RouterFunction<?>> routerFunctions() {
		SortedRouterFunctionsContainer container = new SortedRouterFunctionsContainer();
		obtainApplicationContext().getAutowireCapableBeanFactory().autowireBean(container);

		return CollectionUtils.isEmpty(container.routerFunctions) ? Collections.emptyList() :
				container.routerFunctions;
	}

	@Override
	protected Mono<?> getHandlerInternal(ServerWebExchange exchange) {
		if (this.routerFunction != null) {
			ServerRequest request = ServerRequest.create(exchange, this.messageReaders);
			exchange.getAttributes().put(RouterFunctions.REQUEST_ATTRIBUTE, request);
			return this.routerFunction.route(request);
		}
		else {
			return Mono.empty();
		}
	}


	private static class SortedRouterFunctionsContainer {

		@Nullable
		private List<RouterFunction<?>> routerFunctions;

		@Autowired(required = false)
		public void setRouterFunctions(List<RouterFunction<?>> routerFunctions) {
			this.routerFunctions = routerFunctions;
		}
	}

}
