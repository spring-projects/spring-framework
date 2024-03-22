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

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link DefaultAssertableMvcResult}.
 *
 * @author Stephane Nicoll
 */
class DefaultAssertableMvcResultTests {

	@Test
	void createWithMvcResultDelegatesToIt() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MvcResult mvcResult = mock(MvcResult.class);
		given(mvcResult.getRequest()).willReturn(request);
		DefaultAssertableMvcResult result = new DefaultAssertableMvcResult(mvcResult, null, null);
		assertThat(result.getRequest()).isSameAs(request);
		verify(mvcResult).getRequest();
	}

	@Test
	void createWithExceptionDoesNotAllowAccessToRequest() {
		assertRequestHasFailed(DefaultAssertableMvcResult::getRequest);
	}

	@Test
	void createWithExceptionDoesNotAllowAccessToResponse() {
		assertRequestHasFailed(DefaultAssertableMvcResult::getResponse);
	}

	@Test
	void createWithExceptionDoesNotAllowAccessToHandler() {
		assertRequestHasFailed(DefaultAssertableMvcResult::getHandler);
	}

	@Test
	void createWithExceptionDoesNotAllowAccessToInterceptors() {
		assertRequestHasFailed(DefaultAssertableMvcResult::getInterceptors);
	}

	@Test
	void createWithExceptionDoesNotAllowAccessToModelAndView() {
		assertRequestHasFailed(DefaultAssertableMvcResult::getModelAndView);
	}

	@Test
	void createWithExceptionDoesNotAllowAccessToResolvedException() {
		assertRequestHasFailed(DefaultAssertableMvcResult::getResolvedException);
	}

	@Test
	void createWithExceptionDoesNotAllowAccessToFlashMap() {
		assertRequestHasFailed(DefaultAssertableMvcResult::getFlashMap);
	}

	@Test
	void createWithExceptionDoesNotAllowAccessToAsyncResult() {
		assertRequestHasFailed(DefaultAssertableMvcResult::getAsyncResult);
	}

	@Test
	void createWithExceptionDoesNotAllowAccessToAsyncResultWithTimeToWait() {
		assertRequestHasFailed(result -> result.getAsyncResult(1000));
	}

	@Test
	void createWithExceptionReturnsException() {
		IllegalStateException exception = new IllegalStateException("Expected");
		DefaultAssertableMvcResult result = new DefaultAssertableMvcResult(null, exception, null);
		assertThat(result.getUnresolvedException()).isSameAs(exception);
	}

	private void assertRequestHasFailed(Consumer<DefaultAssertableMvcResult> action) {
		DefaultAssertableMvcResult result = new DefaultAssertableMvcResult(null, new IllegalStateException("Expected"), null);
		assertThatIllegalStateException().isThrownBy(() -> action.accept(result))
				.withMessageContaining("Request has failed with unresolved exception");
	}

}
