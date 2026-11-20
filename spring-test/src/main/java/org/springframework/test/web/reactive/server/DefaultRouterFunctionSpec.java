/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.reactive.server;

import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.server.WebHandler;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

/**
 * Spec for setting up server-less testing against a RouterFunction.
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
class DefaultRouterFunctionSpec extends AbstractMockServerSpec<WebTestClient.RouterFunctionSpec>
		implements WebTestClient.RouterFunctionSpec {

	private final RouterFunction<?> routerFunction;

	private HandlerStrategies handlerStrategies = HandlerStrategies.withDefaults();


	DefaultRouterFunctionSpec(RouterFunction<?> routerFunction) {
		this.routerFunction = routerFunction;
	}


	@Override
	public WebTestClient.RouterFunctionSpec handlerStrategies(HandlerStrategies handlerStrategies) {
		this.handlerStrategies = handlerStrategies;
		return this;
	}

	@Override
	protected WebHttpHandlerBuilder initHttpHandlerBuilder() {
		WebHandler webHandler = RouterFunctions.toWebHandler(this.routerFunction, this.handlerStrategies);
		return WebHttpHandlerBuilder.webHandler(webHandler)
				.filters(filters -> filters.addAll(this.handlerStrategies.webFilters()))
				.exceptionHandlers(handlers -> handlers.addAll(this.handlerStrategies.exceptionHandlers()))
				.localeContextResolver(this.handlerStrategies.localeContextResolver());
	}

}
