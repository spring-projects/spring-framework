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
 * Tests for {@link DefaultMvcTestResult}.
 *
 * @author Stephane Nicoll
 */
class DefaultMvcTestResultTests {

	@Test
	void createWithMvcResultDelegatesToIt() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MvcResult mvcResult = mock(MvcResult.class);
		given(mvcResult.getRequest()).willReturn(request);
		DefaultMvcTestResult result = new DefaultMvcTestResult(mvcResult, null, null);
		assertThat(result.getRequest()).isSameAs(request);
		verify(mvcResult).getRequest();
	}

	@Test
	void createWithExceptionDoesNotAllowAccessToRequest() {
		assertRequestFailed(DefaultMvcTestResult::getRequest);
	}

	@Test
	void createWithExceptionDoesNotAllowAccessToResponse() {
		assertRequestFailed(DefaultMvcTestResult::getResponse);
	}

	@Test
	void createWithExceptionDoesNotAllowAccessToResolvedException() {
		assertRequestFailed(DefaultMvcTestResult::getResolvedException);
	}

	@Test
	void createWithExceptionReturnsException() {
		IllegalStateException exception = new IllegalStateException("Expected");
		DefaultMvcTestResult result = new DefaultMvcTestResult(null, exception, null);
		assertThat(result.getUnresolvedException()).isSameAs(exception);
	}

	private void assertRequestFailed(Consumer<DefaultMvcTestResult> action) {
		DefaultMvcTestResult result = new DefaultMvcTestResult(null, new IllegalStateException("Expected"), null);
		assertThatIllegalStateException()
				.isThrownBy(() -> action.accept(result))
				.withMessageContaining("Request failed with unresolved exception");
	}

}
