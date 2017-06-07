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

package org.springframework.web.reactive.result.method.annotation;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.support.WebBindingInitializer;
import org.springframework.web.bind.support.WebExchangeDataBinder;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.SyncInvocableHandlerMethod;
import org.springframework.web.server.ServerWebExchange;

/**
 * Extends {@link BindingContext} with {@code @InitBinder} method initialization.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class InitBinderBindingContext extends BindingContext {

	private final List<SyncInvocableHandlerMethod> binderMethods;

	/* Simple BindingContext to help with the invoking @InitBinder methods */
	private final BindingContext binderMethodContext;


	InitBinderBindingContext(@Nullable WebBindingInitializer initializer,
			List<SyncInvocableHandlerMethod> binderMethods) {

		super(initializer);
		this.binderMethods = binderMethods;
		this.binderMethodContext = new BindingContext(initializer);
	}


	@Override
	protected WebExchangeDataBinder initDataBinder(WebExchangeDataBinder dataBinder,
			ServerWebExchange exchange) {

		this.binderMethods.stream()
				.filter(binderMethod -> {
					InitBinder ann = binderMethod.getMethodAnnotation(InitBinder.class);
					Assert.state(ann != null, "No InitBinder annotation");
					Collection<String> names = Arrays.asList(ann.value());
					return (names.size() == 0 || names.contains(dataBinder.getObjectName()));
				})
				.forEach(method -> invokeBinderMethod(dataBinder, exchange, method));

		return dataBinder;
	}

	private void invokeBinderMethod(WebExchangeDataBinder dataBinder,
			ServerWebExchange exchange, SyncInvocableHandlerMethod binderMethod) {

		Object returnValue = binderMethod.invokeForHandlerResult(exchange, this.binderMethodContext, dataBinder)
				.getReturnValue();

		if (returnValue != null) {
			throw new IllegalStateException(
					"@InitBinder methods should return void: " + binderMethod);
		}

		// Should not happen (no Model argument resolution) ...
		if (!this.binderMethodContext.getModel().asMap().isEmpty()) {
			throw new IllegalStateException(
					"@InitBinder methods should not add model attributes: " + binderMethod);
		}
	}

}
