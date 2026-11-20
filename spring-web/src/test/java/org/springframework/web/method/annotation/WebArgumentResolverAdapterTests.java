/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.method.annotation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebArgumentResolver;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test fixture with {@link WebArgumentResolverAdapterTests}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
class WebArgumentResolverAdapterTests {

	private WebArgumentResolver adaptee = mock();

	private TestWebArgumentResolverAdapter adapter = new TestWebArgumentResolverAdapter(adaptee);

	private NativeWebRequest webRequest = new ServletWebRequest(new MockHttpServletRequest());

	private MethodParameter parameter;


	@BeforeEach
	void setUp() throws Exception {
		parameter = new MethodParameter(getClass().getMethod("handle", int.class), 0);

		// Expose request to the current thread (for SpEL expressions)
		RequestContextHolder.setRequestAttributes(webRequest);
	}

	@AfterEach
	void resetRequestContextHolder() {
		RequestContextHolder.resetRequestAttributes();
	}

	@Test
	void supportsParameter() throws Exception {
		given(adaptee.resolveArgument(parameter, webRequest)).willReturn(42);

		assertThat(adapter.supportsParameter(parameter)).as("Parameter not supported").isTrue();

		verify(adaptee).resolveArgument(parameter, webRequest);
	}

	@Test
	void supportsParameterUnresolved() throws Exception {
		given(adaptee.resolveArgument(parameter, webRequest)).willReturn(WebArgumentResolver.UNRESOLVED);

		assertThat(adapter.supportsParameter(parameter)).as("Parameter supported").isFalse();

		verify(adaptee).resolveArgument(parameter, webRequest);
	}

	@Test
	void supportsParameterWrongType() throws Exception {
		given(adaptee.resolveArgument(parameter, webRequest)).willReturn("Foo");

		assertThat(adapter.supportsParameter(parameter)).as("Parameter supported").isFalse();

		verify(adaptee).resolveArgument(parameter, webRequest);
	}

	@Test
	void supportsParameterThrowsException() throws Exception {
		given(adaptee.resolveArgument(parameter, webRequest)).willThrow(new Exception());

		assertThat(adapter.supportsParameter(parameter)).as("Parameter supported").isFalse();

		verify(adaptee).resolveArgument(parameter, webRequest);
	}

	@Test
	void resolveArgument() throws Exception {
		int expected = 42;
		given(adaptee.resolveArgument(parameter, webRequest)).willReturn(expected);

		Object result = adapter.resolveArgument(parameter, null, webRequest, null);
		assertThat(result).as("Invalid result").isEqualTo(expected);
	}

	@Test
	void resolveArgumentUnresolved() throws Exception {
		given(adaptee.resolveArgument(parameter, webRequest)).willReturn(WebArgumentResolver.UNRESOLVED);

		assertThatIllegalStateException().isThrownBy(() ->
				adapter.resolveArgument(parameter, null, webRequest, null));
	}

	@Test
	void resolveArgumentWrongType() throws Exception {
		given(adaptee.resolveArgument(parameter, webRequest)).willReturn("Foo");

		assertThatIllegalStateException().isThrownBy(() ->
				adapter.resolveArgument(parameter, null, webRequest, null));
	}

	@Test
	void resolveArgumentThrowsException() throws Exception {
		given(adaptee.resolveArgument(parameter, webRequest)).willThrow(new Exception());

		assertThatException().isThrownBy(() -> adapter.resolveArgument(parameter, null, webRequest, null));
	}

	public void handle(int param) {
	}

	private class TestWebArgumentResolverAdapter extends AbstractWebArgumentResolverAdapter {

		TestWebArgumentResolverAdapter(WebArgumentResolver adaptee) {
			super(adaptee);
		}

		@Override
		protected NativeWebRequest getWebRequest() {
			return WebArgumentResolverAdapterTests.this.webRequest;
		}
	}

}
