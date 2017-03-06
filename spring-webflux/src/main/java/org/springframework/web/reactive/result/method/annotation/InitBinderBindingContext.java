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
import java.util.Optional;

import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.support.WebBindingInitializer;
import org.springframework.web.bind.support.WebExchangeDataBinder;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.SyncInvocableHandlerMethod;
import org.springframework.web.server.ServerWebExchange;

/**
 * Variant of {@link BindingContext} that further initializes {@code DataBinder}
 * instances through {@code @InitBinder} methods.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class InitBinderBindingContext extends BindingContext {

	private final List<SyncInvocableHandlerMethod> binderMethods;

	/* Simple BindingContext to help with the invoking @InitBinder methods */
	private final BindingContext binderMethodContext;


	public InitBinderBindingContext(WebBindingInitializer initializer,
			List<SyncInvocableHandlerMethod> binderMethods) {

		super(initializer);
		this.binderMethods = binderMethods;
		this.binderMethodContext = new BindingContext(initializer);
	}


	@Override
	protected WebExchangeDataBinder initDataBinder(WebExchangeDataBinder binder, ServerWebExchange exchange) {

		this.binderMethods.stream()
				.filter(binderMethod -> {
					InitBinder annotation = binderMethod.getMethodAnnotation(InitBinder.class);
					Collection<String> names = Arrays.asList(annotation.value());
					return (names.size() == 0 || names.contains(binder.getObjectName()));
				})
				.forEach(method -> invokeBinderMethod(binder, exchange, method));

		return binder;
	}

	private void invokeBinderMethod(WebExchangeDataBinder binder, ServerWebExchange exchange,
			SyncInvocableHandlerMethod binderMethod) {

		Optional<Object> returnValue = binderMethod
				.invokeForHandlerResult(exchange, this.binderMethodContext, binder)
				.getReturnValue();

		if (returnValue.isPresent()) {
			throw new IllegalStateException(
					"@InitBinder methods should return void: " + binderMethod);
		}

		// Should not happen (no argument resolvers)...

		if (!this.binderMethodContext.getModel().asMap().isEmpty()) {
			throw new IllegalStateException(
					"@InitBinder methods should not add model attributes: " + binderMethod);
		}
	}

}
