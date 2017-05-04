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

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.reactive.HandlerAdapter;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.HandlerResultHandler;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.support.HandlerFunctionAdapter;
import org.springframework.web.reactive.function.server.support.ServerResponseResultHandler;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

/**
 * Spec for setting up server-less testing against a RouterFunction.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class RouterFunctionSpec extends AbstractMockServerSpec<RouterFunctionSpec> {

	private final RouterFunction<?> routerFunction;


	RouterFunctionSpec(RouterFunction<?> routerFunction) {
		this.routerFunction = routerFunction;
	}


	@Override
	protected WebHttpHandlerBuilder initHttpHandlerBuilder() {
		return WebHttpHandlerBuilder.applicationContext(initApplicationContext());
	}

	private ApplicationContext initApplicationContext() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.registerBean("webHandler", DispatcherHandler.class, () -> new DispatcherHandler());
		context.registerBean(HandlerMapping.class, () -> RouterFunctions.toHandlerMapping(this.routerFunction));
		context.registerBean(HandlerAdapter.class, () -> new HandlerFunctionAdapter());
		context.registerBean(HandlerResultHandler.class, () -> new ServerResponseResultHandler());
		context.refresh();
		return context;
	}

}
