/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.method.support;

import java.lang.reflect.Method;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.testfixture.method.ResolvableMethod;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatRuntimeException;

/**
 * Tests for {@link InvocableHandlerMethod}.
 *
 * @author Rossen Stoyanchev
 */
class InvocableHandlerMethodTests {

	private NativeWebRequest request;

	private final HandlerMethodArgumentResolverComposite composite = new HandlerMethodArgumentResolverComposite();


	@BeforeEach
	void setUp() {
		this.request = new ServletWebRequest(new MockHttpServletRequest(), new MockHttpServletResponse());
	}


	@Test
	void resolveArg() throws Exception {
		this.composite.addResolver(new StubArgumentResolver(99));
		this.composite.addResolver(new StubArgumentResolver("value"));

		Object value = getInvocable(Integer.class, String.class).invokeForRequest(request, null);

		assertThat(getStubResolver(0).getResolvedParameters()).hasSize(1);
		assertThat(getStubResolver(1).getResolvedParameters()).hasSize(1);
		assertThat(value).isEqualTo("99-value");
		assertThat(getStubResolver(0).getResolvedParameters().get(0).getParameterName()).isEqualTo("intArg");
		assertThat(getStubResolver(1).getResolvedParameters().get(0).getParameterName()).isEqualTo("stringArg");
	}

	@Test
	void resolveNoArgValue() throws Exception {
		this.composite.addResolver(new StubArgumentResolver(Integer.class));
		this.composite.addResolver(new StubArgumentResolver(String.class));

		Object returnValue = getInvocable(Integer.class, String.class).invokeForRequest(request, null);

		assertThat(getStubResolver(0).getResolvedParameters()).hasSize(1);
		assertThat(getStubResolver(1).getResolvedParameters()).hasSize(1);
		assertThat(returnValue).isEqualTo("null-null");
	}

	@Test
	void cannotResolveArg() {
		assertThatIllegalStateException().isThrownBy(() ->
				getInvocable(Integer.class, String.class).invokeForRequest(request, null))
			.withMessageContaining("Could not resolve parameter [0]");
	}

	@Test
	void resolveProvidedArg() throws Exception {
		Object value = getInvocable(Integer.class, String.class).invokeForRequest(request, null, 99, "value");

		assertThat(value).isNotNull();
		assertThat(value.getClass()).isEqualTo(String.class);
		assertThat(value).isEqualTo("99-value");
	}

	@Test
	void resolveProvidedArgFirst() throws Exception {
		this.composite.addResolver(new StubArgumentResolver(1));
		this.composite.addResolver(new StubArgumentResolver("value1"));
		Object value = getInvocable(Integer.class, String.class).invokeForRequest(request, null, 2, "value2");

		assertThat(value).isEqualTo("2-value2");
	}

	@Test
	void resolveHandlerMethodArgToNull() throws Exception {
		Object value = getInvocable(HandlerMethod.class).invokeForRequest(request, null);

		assertThat(value).isNotNull();
		assertThat(value).isEqualTo("success");
	}

	@Test
	void exceptionInResolvingArg() {
		this.composite.addResolver(new ExceptionRaisingArgumentResolver());
		assertThatIllegalArgumentException().isThrownBy(() ->
				getInvocable(Integer.class, String.class).invokeForRequest(request, null));
	}

	@Test
	void illegalArgumentException() {
		this.composite.addResolver(new StubArgumentResolver(Integer.class, "__not_an_int__"));
		this.composite.addResolver(new StubArgumentResolver("value"));
		assertThatIllegalStateException().isThrownBy(() ->
				getInvocable(Integer.class, String.class).invokeForRequest(request, null))
			.withCauseInstanceOf(IllegalArgumentException.class)
			.withMessageContaining("Controller [")
			.withMessageContaining("Method [")
			.withMessageContaining("with argument values:")
			.withMessageContaining("[0] [type=java.lang.String] [value=__not_an_int__]")
			.withMessageContaining("[1] [type=java.lang.String] [value=value");
	}

	@Test
	void invocationTargetException() {
		RuntimeException runtimeException = new RuntimeException("error");
		assertThatRuntimeException()
			.isThrownBy(() -> getInvocable(Throwable.class).invokeForRequest(this.request, null, runtimeException))
			.isSameAs(runtimeException);

		Error error = new Error("error");
		assertThatExceptionOfType(Error.class)
			.isThrownBy(() -> getInvocable(Throwable.class).invokeForRequest(this.request, null, error))
			.isSameAs(error);

		Exception exception = new Exception("error");
		assertThatException()
			.isThrownBy(() -> getInvocable(Throwable.class).invokeForRequest(this.request, null, exception))
			.isSameAs(exception);

		Throwable throwable = new Throwable("error");
		assertThatIllegalStateException()
			.isThrownBy(() -> getInvocable(Throwable.class).invokeForRequest(this.request, null, throwable))
			.withCause(throwable)
			.withMessageContaining("Invocation failure");
	}

	@Test  // SPR-13917
	public void invocationErrorMessage() {
		this.composite.addResolver(new StubArgumentResolver(double.class));
		assertThatIllegalStateException()
			.isThrownBy(() -> getInvocable(double.class).invokeForRequest(this.request, null))
			.withMessageContaining("Illegal argument");
	}

	private InvocableHandlerMethod getInvocable(Class<?>... argTypes) {
		Method method = ResolvableMethod.on(Handler.class).argTypes(argTypes).resolveMethod();
		InvocableHandlerMethod handlerMethod = new InvocableHandlerMethod(new Handler(), method);
		handlerMethod.setHandlerMethodArgumentResolvers(this.composite);
		return handlerMethod;
	}

	private StubArgumentResolver getStubResolver(int index) {
		return (StubArgumentResolver) this.composite.getResolvers().get(index);
	}



	@SuppressWarnings("unused")
	private static class Handler {

		public String handle(Integer intArg, String stringArg) {
			return intArg + "-" + stringArg;
		}

		public void handle(double amount) {
		}

		public void handleWithException(Throwable ex) throws Throwable {
			throw ex;
		}

		public String handleHandlerMethod(@Nullable HandlerMethod handlerMethod) {
			return "success";
		}
	}


	private static class ExceptionRaisingArgumentResolver implements HandlerMethodArgumentResolver {

		@Override
		public boolean supportsParameter(MethodParameter parameter) {
			return true;
		}

		@Override
		public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
				NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {

			throw new IllegalArgumentException("oops, can't read");
		}
	}

}
