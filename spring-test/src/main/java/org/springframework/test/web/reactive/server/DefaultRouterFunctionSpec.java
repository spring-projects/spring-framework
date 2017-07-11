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
		return WebHttpHandlerBuilder.webHandler(webHandler);
	}

}
