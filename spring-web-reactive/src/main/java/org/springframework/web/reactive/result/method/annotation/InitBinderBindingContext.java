/*
 * Copyright 2002-2016 the original author or authors.
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

import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.WebExchangeDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.support.WebBindingInitializer;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.result.method.SyncInvocableHandlerMethod;
import org.springframework.web.server.ServerWebExchange;

/**
 * An extension of {@link BindingContext} that uses {@code @InitBinder} methods
 * to initialize a data binder instance.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class InitBinderBindingContext extends BindingContext {

	private final List<SyncInvocableHandlerMethod> binderMethods;

	/** BindingContext for @InitBinder method invocation */
	private final BindingContext bindingContext;


	public InitBinderBindingContext(WebBindingInitializer initializer,
			List<SyncInvocableHandlerMethod> binderMethods) {

		super(initializer);
		this.binderMethods = binderMethods;
		this.bindingContext = new BindingContext(initializer);
	}


	@Override
	protected WebExchangeDataBinder initDataBinder(WebExchangeDataBinder binder,
			ServerWebExchange exchange) {

		this.binderMethods.stream()
				.filter(method -> isBinderMethodApplicable(method, binder))
				.forEach(method -> invokeInitBinderMethod(binder, exchange, method));

		return binder;
	}

	/**
	 * Whether the given {@code @InitBinder} method should be used to initialize
	 * the given WebDataBinder instance. By default we check the attributes
	 * names of the annotation, if present.
	 */
	protected boolean isBinderMethodApplicable(HandlerMethod binderMethod, WebDataBinder binder) {
		InitBinder annot = binderMethod.getMethodAnnotation(InitBinder.class);
		Collection<String> names = Arrays.asList(annot.value());
		return (names.size() == 0 || names.contains(binder.getObjectName()));
	}

	private void invokeInitBinderMethod(WebExchangeDataBinder binder, ServerWebExchange exchange,
			SyncInvocableHandlerMethod method) {

		HandlerResult result = method.invokeForHandlerResult(exchange, this.bindingContext, binder);
		if (result.getReturnValue().isPresent()) {
			throw new IllegalStateException("@InitBinder methods should return void: " + method);
		}
	}

}
