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

package org.springframework.web.reactive;

import java.util.function.Function;

import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.ui.Model;
import org.springframework.util.Assert;

/**
 * Represent the result of the invocation of a handler or a handler method.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class HandlerResult {

	private final Object handler;

	@Nullable
	private final Object returnValue;

	private final ResolvableType returnType;

	private final BindingContext bindingContext;

	@Nullable
	private Function<Throwable, Mono<HandlerResult>> exceptionHandler;


	/**
	 * Create a new {@code HandlerResult}.
	 * @param handler the handler that handled the request
	 * @param returnValue the return value from the handler possibly {@code null}
	 * @param returnType the return value type
	 */
	public HandlerResult(Object handler, @Nullable Object returnValue, MethodParameter returnType) {
		this(handler, returnValue, returnType, null);
	}

	/**
	 * Create a new {@code HandlerResult}.
	 * @param handler the handler that handled the request
	 * @param returnValue the return value from the handler possibly {@code null}
	 * @param returnType the return value type
	 * @param context the binding context used for request handling
	 */
	public HandlerResult(Object handler, @Nullable Object returnValue, MethodParameter returnType,
			@Nullable BindingContext context) {

		Assert.notNull(handler, "'handler' is required");
		Assert.notNull(returnType, "'returnType' is required");
		this.handler = handler;
		this.returnValue = returnValue;
		this.returnType = ResolvableType.forMethodParameter(returnType);
		this.bindingContext = (context != null ? context : new BindingContext());
	}


	/**
	 * Return the handler that handled the request.
	 */
	public Object getHandler() {
		return this.handler;
	}

	/**
	 * Return the value returned from the handler, if any.
	 */
	@Nullable
	public Object getReturnValue() {
		return this.returnValue;
	}

	/**
	 * Return the type of the value returned from the handler -- e.g. the return
	 * type declared on a controller method's signature. Also see
	 * {@link #getReturnTypeSource()} to obtain the underlying
	 * {@link MethodParameter} for the return type.
	 */
	public ResolvableType getReturnType() {
		return this.returnType;
	}

	/**
	 * Return the {@link MethodParameter} from which {@link #getReturnType()
	 * returnType} was created.
	 */
	public MethodParameter getReturnTypeSource() {
		return (MethodParameter) this.returnType.getSource();
	}

	/**
	 * Return the BindingContext used for request handling.
	 */
	public BindingContext getBindingContext() {
		return this.bindingContext;
	}

	/**
	 * Return the model used for request handling. This is a shortcut for
	 * {@code getBindingContext().getModel()}.
	 */
	public Model getModel() {
		return this.bindingContext.getModel();
	}

	/**
	 * Configure an exception handler that may be used to produce an alternative
	 * result when result handling fails. Especially for an async return value
	 * errors may occur after the invocation of the handler.
	 * @param function the error handler
	 * @return the current instance
	 */
	public HandlerResult setExceptionHandler(Function<Throwable, Mono<HandlerResult>> function) {
		this.exceptionHandler = function;
		return this;
	}

	/**
	 * Whether there is an exception handler.
	 */
	public boolean hasExceptionHandler() {
		return (this.exceptionHandler != null);
	}

	/**
	 * Apply the exception handler and return the alternative result.
	 * @param failure the exception
	 * @return the new result or the same error if there is no exception handler
	 */
	public Mono<HandlerResult> applyExceptionHandler(Throwable failure) {
		return (this.exceptionHandler != null ? this.exceptionHandler.apply(failure) : Mono.error(failure));
	}

}
