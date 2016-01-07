/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.reactive;

import java.util.function.Function;

import reactor.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.util.Assert;

/**
 * Represent the result of the invocation of a handler.
 *
 * @author Rossen Stoyanchev
 */
public class HandlerResult {

	private final Object handler;

	private final Object result;

	private final ResolvableType resultType;

	private Function<Throwable, Mono<HandlerResult>> exceptionHandler;


	public HandlerResult(Object handler, Object result, ResolvableType resultType) {
		Assert.notNull(handler, "'handler' is required");
		Assert.notNull(handler, "'resultType' is required");
		this.handler = handler;
		this.result = result;
		this.resultType = resultType;
	}


	public Object getHandler() {
		return this.handler;
	}

	public Object getResult() {
		return this.result;
	}

	public ResolvableType getResultType() {
		return this.resultType;
	}

	/**
	 * For an async result, failures may occur later during result handling.
	 * Use this property to configure an exception handler to be invoked if
	 * result handling fails.
	 *
	 * @param function a function to map the the error to an alternative result.
	 * @return the current instance
	 */
	public HandlerResult setExceptionHandler(Function<Throwable, Mono<HandlerResult>> function) {
		this.exceptionHandler = function;
		return this;
	}

	public boolean hasExceptionHandler() {
		return (this.exceptionHandler != null);
	}

	public Mono<HandlerResult> applyExceptionHandler(Throwable ex) {
		return (hasExceptionHandler() ? this.exceptionHandler.apply(ex) : Mono.error(ex));
	}

}
