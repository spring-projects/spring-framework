/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.web.servlet.assertj;

import java.lang.reflect.Method;

import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;

import org.springframework.cglib.core.internal.Function;
import org.springframework.lang.Nullable;
import org.springframework.test.util.MethodAssert;
import org.springframework.util.ClassUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.MethodInvocationInfo;

/**
 * AssertJ {@linkplain org.assertj.core.api.Assert assertions} that can be applied
 * to a handler or handler method.

 * @author Stephane Nicoll
 * @since 6.2
 */
public class HandlerResultAssert extends AbstractObjectAssert<HandlerResultAssert, Object> {

	public HandlerResultAssert(@Nullable Object actual) {
		super(actual, HandlerResultAssert.class);
		as("Handler result");
	}

	/**
	 * Return a new {@linkplain MethodAssert assertion} object that uses
	 * the {@link Method} that handles the request as the object to test.
	 * <p>Verifies first that the handler is a {@linkplain #isMethodHandler()
	 * method handler}.
	 * <p>Example: <pre><code class="java">
	 * // Check that a GET to "/greet" is invoked on a "handleGreet" method name
	 * assertThat(mvc.perform(get("/greet")).handler().method().hasName("handleGreet");
	 * </code></pre>
	 */
	public MethodAssert method() {
		return new MethodAssert(getHandlerMethod());
	}

	/**
	 * Verify that the handler is managed by a method invocation, typically on
	 * a controller.
	 */
	public HandlerResultAssert isMethodHandler() {
		return isNotNull().isInstanceOf(HandlerMethod.class);
	}

	/**
	 * Verify that the handler is managed by the given {@code handlerMethod}.
	 * <p>This creates a "mock" for the given {@code controllerType} and records
	 * the method invocation in the {@code handlerMethod}. The arguments used by
	 * the target method invocation can be {@code null} as the purpose of the mock
	 * is to identify the method that was invoked.
	 * <p>Example: <pre><code class="java">
	 * // If the method has a return type, you can return the result of the invocation
	 * assertThat(mvc.perform(get("/greet")).handler().isInvokedOn(
	 *         GreetController.class, controller -> controller.sayGreet());
	 *
	 * // If the method has a void return type, the controller should be returned
	 * assertThat(mvc.perform(post("/persons/")).handler().isInvokedOn(
	 *         PersonController.class, controller -> controller.createPerson(null, null));
	 * </code></pre>
	 * @param controllerType the controller to mock
	 * @param handlerMethod the method
	 */
	public <T> HandlerResultAssert isInvokedOn(Class<T> controllerType, Function<T, Object> handlerMethod) {
		MethodAssert actual = method();
		Object methodInvocationInfo = handlerMethod.apply(MvcUriComponentsBuilder.on(controllerType));
		Assertions.assertThat(methodInvocationInfo)
				.as("Method invocation on controller '%s'", controllerType.getSimpleName())
				.isInstanceOfSatisfying(MethodInvocationInfo.class, mii ->
						actual.isEqualTo(mii.getControllerMethod()));
		return this;
	}

	/**
	 * Verify that the handler is of the given {@code type}. For a controller
	 * method, this is the type of the controller.
	 * <p>Example: <pre><code class="java">
	 * // Check that a GET to "/greet" is managed by GreetController
	 * assertThat(mvc.perform(get("/greet")).handler().hasType(GreetController.class);
	 * </code></pre>
	 * @param type the expected type of the handler
	 */
	public HandlerResultAssert hasType(Class<?> type) {
		isNotNull();
		Class<?> actualType = this.actual.getClass();
		if (this.actual instanceof HandlerMethod handlerMethod) {
			actualType = handlerMethod.getBeanType();
		}
		Assertions.assertThat(ClassUtils.getUserClass(actualType)).as("Handler result type").isEqualTo(type);
		return this;
	}

	private Method getHandlerMethod() {
		isMethodHandler(); // validate type
		return ((HandlerMethod) this.actual).getMethod();
	}


}
