/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive;

import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
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

	private final @Nullable Object returnValue;

	private final ResolvableType returnType;

	private final BindingContext bindingContext;

	private @Nullable DispatchExceptionHandler exceptionHandler;


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
	public @Nullable Object getReturnValue() {
		return this.returnValue;
	}

	/**
	 * Return the type of the value returned from the handler -- for example, the return
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
	 * {@link HandlerAdapter} classes can set this to have their exception
	 * handling mechanism applied to response rendering and to deferred
	 * exceptions when invoking a handler with an asynchronous return value.
	 * @param exceptionHandler the exception handler to use
	 * @since 6.0
	 */
	public HandlerResult setExceptionHandler(DispatchExceptionHandler exceptionHandler) {
		this.exceptionHandler = exceptionHandler;
		return this;
	}

	/**
	 * Return the {@link #setExceptionHandler(DispatchExceptionHandler)
	 * configured} exception handler.
	 * @since 6.0
	 */
	public @Nullable DispatchExceptionHandler getExceptionHandler() {
		return this.exceptionHandler;
	}

}
