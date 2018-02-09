/*
 * Copyright 2002-2018 the original author or authors.
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
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.bind.support.SimpleSessionStatus;
import org.springframework.web.bind.support.WebBindingInitializer;
import org.springframework.web.bind.support.WebExchangeDataBinder;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.result.method.SyncInvocableHandlerMethod;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;

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

	private final SessionStatus sessionStatus = new SimpleSessionStatus();

	@Nullable
	private Runnable saveModelOperation;


	InitBinderBindingContext(@Nullable WebBindingInitializer initializer,
			List<SyncInvocableHandlerMethod> binderMethods) {

		super(initializer);
		this.binderMethods = binderMethods;
		this.binderMethodContext = new BindingContext(initializer);
	}


	/**
	 * Return the {@link SessionStatus} instance to use that can be used to
	 * signal that session processing is complete.
	 */
	public SessionStatus getSessionStatus() {
		return this.sessionStatus;
	}


	@Override
	protected WebExchangeDataBinder initDataBinder(WebExchangeDataBinder dataBinder,
			ServerWebExchange exchange) {

		this.binderMethods.stream()
				.filter(binderMethod -> {
					InitBinder ann = binderMethod.getMethodAnnotation(InitBinder.class);
					Assert.state(ann != null, "No InitBinder annotation");
					Collection<String> names = Arrays.asList(ann.value());
					return (names.isEmpty() || names.contains(dataBinder.getObjectName()));
				})
				.forEach(method -> invokeBinderMethod(dataBinder, exchange, method));

		return dataBinder;
	}

	private void invokeBinderMethod(WebExchangeDataBinder dataBinder,
			ServerWebExchange exchange, SyncInvocableHandlerMethod binderMethod) {

		HandlerResult result = binderMethod.invokeForHandlerResult(
				exchange, this.binderMethodContext, dataBinder);

		if (result != null && result.getReturnValue() != null) {
			throw new IllegalStateException(
					"@InitBinder methods should return void: " + binderMethod);
		}

		// Should not happen (no Model argument resolution) ...
		if (!this.binderMethodContext.getModel().asMap().isEmpty()) {
			throw new IllegalStateException(
					"@InitBinder methods should not add model attributes: " + binderMethod);
		}
	}

	/**
	 * Provide the context required to apply {@link #saveModel()} after the
	 * controller method has been invoked.
	 */
	public void setSessionContext(SessionAttributesHandler attributesHandler, WebSession session) {
		this.saveModelOperation = () -> {
			if (getSessionStatus().isComplete()) {
				attributesHandler.cleanupAttributes(session);
			}
			else {
				attributesHandler.storeAttributes(session, getModel().asMap());
			}
		};
	}

	/**
	 * Save model attributes in the session based on a type-level declarations
	 * in an {@code @SessionAttributes} annotation.
	 */
	public void saveModel() {
		if (this.saveModelOperation != null) {
			this.saveModelOperation.run();
		}
	}

}
