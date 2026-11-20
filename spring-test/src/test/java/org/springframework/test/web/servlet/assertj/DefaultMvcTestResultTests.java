/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
