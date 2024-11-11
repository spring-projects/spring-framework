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

package org.springframework.web.context.request;

import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import io.micrometer.context.ContextRegistry;
import io.micrometer.context.ContextSnapshot;
import io.micrometer.context.ContextSnapshot.Scope;
import io.micrometer.context.ContextSnapshotFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST;

/**
 * Tests for {@link RequestAttributesThreadLocalAccessor}.
 *
 * @author Tadaya Tsuyukubo
 * @author Rossen Stoyanchev
 */
class RequestAttributesThreadLocalAccessorTests {

	private final ContextRegistry registry =
			new ContextRegistry().registerThreadLocalAccessor(new RequestAttributesThreadLocalAccessor());


	private static Stream<Arguments> propagation() {
		RequestAttributes previous = mock();
		RequestAttributes current = mock();
		return Stream.of(arguments(null, current), arguments(previous, current));
	}

	@ParameterizedTest
	@MethodSource
	@SuppressWarnings({ "try", "unused" })
	void propagation(RequestAttributes previousRequest, RequestAttributes currentRequest) throws Exception {
		ContextSnapshot snapshot = getSnapshotFor(currentRequest);

		AtomicReference<RequestAttributes> requestInScope = new AtomicReference<>();
		AtomicReference<RequestAttributes> requestAfterScope = new AtomicReference<>();

		Thread thread = new Thread(() -> {
			RequestContextHolder.setRequestAttributes(previousRequest);
			try (Scope scope = snapshot.setThreadLocals()) {
				requestInScope.set(RequestContextHolder.getRequestAttributes());
			}
			requestAfterScope.set(RequestContextHolder.getRequestAttributes());
		});

		thread.start();
		thread.join(1000);

		assertThat(requestInScope).hasValueSatisfying(value -> assertThat(value).isSameAs(currentRequest));
		assertThat(requestAfterScope).hasValueSatisfying(value -> assertThat(value).isSameAs(previousRequest));
	}

	@Test
	@SuppressWarnings("try")
	void accessAfterRequestMarkedCompleted() {
		MockHttpServletRequest servletRequest = new MockHttpServletRequest();
		servletRequest.setAttribute("k1", "v1");
		servletRequest.setAttribute("k2", "v2");

		ServletRequestAttributes attributes = new ServletRequestAttributes(servletRequest, new MockHttpServletResponse());
		ContextSnapshot snapshot = getSnapshotFor(attributes);
		attributes.requestCompleted(); // REQUEST dispatch ends, async handling continues

		try (Scope scope = snapshot.setThreadLocals()) {
			RequestAttributes current = RequestContextHolder.getRequestAttributes();
			assertThat(current).isNotNull();
			assertThat(current.getAttributeNames(SCOPE_REQUEST)).containsExactly("k1", "k2");
			assertThat(current.getAttribute("k1", SCOPE_REQUEST)).isEqualTo("v1");
			assertThat(current.getAttribute("k2", SCOPE_REQUEST)).isEqualTo("v2");
			assertThatIllegalStateException().isThrownBy(() -> current.setAttribute("k3", "v3", SCOPE_REQUEST));
		}
	}

	@Test
	@SuppressWarnings("try")
	void accessBeforeRequestMarkedCompleted() {
		MockHttpServletRequest servletRequest = new MockHttpServletRequest();
		ServletRequestAttributes previous = new ServletRequestAttributes(servletRequest, new MockHttpServletResponse());

		ContextSnapshot snapshot = getSnapshotFor(previous);

		RequestContextHolder.setRequestAttributes(previous);
		try {
			try (Scope scope = snapshot.setThreadLocals()) {
				RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
				assertThat(attributes).isNotNull();
				attributes.setAttribute("k1", "v1", SCOPE_REQUEST);
			}
			RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
			assertThat(attributes).isNotNull();
			attributes.setAttribute("k2", "v2", SCOPE_REQUEST);
		}
		finally {
			RequestContextHolder.resetRequestAttributes();
		}

		assertThat(previous.getAttributeNames(SCOPE_REQUEST)).containsExactly("k1", "k2");
		assertThat(previous.getAttribute("k1", SCOPE_REQUEST)).isEqualTo("v1");
		assertThat(previous.getAttribute("k2", SCOPE_REQUEST)).isEqualTo("v2");
	}

	private ContextSnapshot getSnapshotFor(RequestAttributes request) {
		RequestContextHolder.setRequestAttributes(request);
		try {
			return ContextSnapshotFactory.builder()
					.contextRegistry(this.registry).clearMissing(true).build()
					.captureAll();
		}
		finally {
			RequestContextHolder.resetRequestAttributes();
		}
	}

}
