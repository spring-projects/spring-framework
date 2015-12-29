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
import java.util.logging.Handler;

import org.reactivestreams.Publisher;
import reactor.Publishers;

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

	private final Throwable error;

	private Function<Throwable, Publisher<HandlerResult>> exceptionMapper;


	public HandlerResult(Object handler, Object result, ResolvableType resultType) {
		Assert.notNull(handler, "'handler' is required");
		Assert.notNull(handler, "'resultType' is required");
		this.handler = handler;
		this.result = result;
		this.resultType = resultType;
		this.error = null;
	}

	public HandlerResult(Object handler, Throwable error) {
		Assert.notNull(handler, "'handler' is required");
		Assert.notNull(error, "'error' is required");
		this.handler = handler;
		this.result = null;
		this.resultType = null;
		this.error = error;
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

	public Throwable getError() {
		return this.error;
	}

	/**
	 * Whether handler invocation produced a result or failed with an error.
	 * <p>If {@code true} the {@link #getError()} returns the error while
	 * {@link #getResult()} and {@link #getResultType()} return {@code null}
	 * and vice versa.
	 * @return whether this instance contains a result or an error.
	 */
	public boolean hasError() {
		return (this.error != null);
	}

	/**
	 * Configure a function for selecting an alternate {@code HandlerResult} in
	 * case of an {@link #hasError() error result} or in case of an async result
	 * that results in an error.
	 * @param function the exception resolving function
	 */
	public HandlerResult setExceptionMapper(Function<Throwable, Publisher<HandlerResult>> function) {
		this.exceptionMapper = function;
		return this;
	}

	public Function<Throwable, Publisher<HandlerResult>> getExceptionMapper() {
		return this.exceptionMapper;
	}

	public boolean hasExceptionMapper() {
		return (this.exceptionMapper != null);
	}

}
