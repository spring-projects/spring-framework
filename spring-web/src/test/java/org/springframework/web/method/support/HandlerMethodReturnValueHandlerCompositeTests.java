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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.MethodParameter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Test fixture with {@link HandlerMethodReturnValueHandlerComposite}.
 *
 * @author Rossen Stoyanchev
 */
@SuppressWarnings("unused")
class HandlerMethodReturnValueHandlerCompositeTests {

	private HandlerMethodReturnValueHandlerComposite handlers = new HandlerMethodReturnValueHandlerComposite();

	private HandlerMethodReturnValueHandler integerHandler = mock();

	private ModelAndViewContainer mavContainer = new ModelAndViewContainer();

	private MethodParameter integerType;

	private MethodParameter stringType;


	@BeforeEach
	void setup() throws Exception {
		this.integerType = new MethodParameter(getClass().getDeclaredMethod("handleInteger"), -1);
		this.stringType = new MethodParameter(getClass().getDeclaredMethod("handleString"), -1);

		given(this.integerHandler.supportsReturnType(this.integerType)).willReturn(true);

		this.handlers.addHandler(this.integerHandler);
	}


	@Test
	void supportsReturnType() {
		assertThat(this.handlers.supportsReturnType(this.integerType)).isTrue();
		assertThat(this.handlers.supportsReturnType(this.stringType)).isFalse();
	}

	@Test
	void handleReturnValue() throws Exception {
		this.handlers.handleReturnValue(55, this.integerType, this.mavContainer, null);
		verify(this.integerHandler).handleReturnValue(55, this.integerType, this.mavContainer, null);
	}

	@Test
	void handleReturnValueWithMultipleHandlers() throws Exception {
		HandlerMethodReturnValueHandler anotherIntegerHandler = mock();
		given(anotherIntegerHandler.supportsReturnType(this.integerType)).willReturn(true);

		this.handlers.handleReturnValue(55, this.integerType, this.mavContainer, null);

		verify(this.integerHandler).handleReturnValue(55, this.integerType, this.mavContainer, null);
		verifyNoMoreInteractions(anotherIntegerHandler);
	}

	@Test  // SPR-13083
	void handleReturnValueWithAsyncHandler() throws Exception {
		Promise<Integer> promise = new Promise<>();
		MethodParameter promiseType = new MethodParameter(getClass().getDeclaredMethod("handlePromise"), -1);

		HandlerMethodReturnValueHandler responseBodyHandler = mock();
		given(responseBodyHandler.supportsReturnType(promiseType)).willReturn(true);
		this.handlers.addHandler(responseBodyHandler);

		AsyncHandlerMethodReturnValueHandler promiseHandler = mock();
		given(promiseHandler.supportsReturnType(promiseType)).willReturn(true);
		given(promiseHandler.isAsyncReturnValue(promise, promiseType)).willReturn(true);
		this.handlers.addHandler(promiseHandler);

		this.handlers.handleReturnValue(promise, promiseType, this.mavContainer, null);

		verify(promiseHandler).isAsyncReturnValue(promise, promiseType);
		verify(promiseHandler).supportsReturnType(promiseType);
		verify(promiseHandler).handleReturnValue(promise, promiseType, this.mavContainer, null);
		verifyNoMoreInteractions(promiseHandler);
		verifyNoMoreInteractions(responseBodyHandler);
	}

	@Test
	void noSuitableReturnValueHandler() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				this.handlers.handleReturnValue("value", this.stringType, null, null));
	}


	private Integer handleInteger() {
		return null;
	}

	private String handleString() {
		return null;
	}

	private Promise<Integer> handlePromise() {
		return null;
	}

	private static class Promise<T> {}

}
