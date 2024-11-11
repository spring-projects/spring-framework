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

package org.springframework.test.web.servlet.request;

import java.net.URI;

import jakarta.servlet.ServletContext;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AbstractMockHttpServletRequestBuilder}
 *
 * @author Stephane Nicoll
 */
class AbstractMockHttpServletRequestBuilderTests {

	private final ServletContext servletContext = new MockServletContext();

	@Test
	void uriTemplateSetsRequestsUrlAndTemplateConsistently() {
		MockHttpServletRequest request = buildRequest(new TestRequestBuilder(HttpMethod.GET).uri("/hotels/{id}", 42));
		assertThat(request.getRequestURL().toString()).isEqualTo("http://localhost/hotels/42");
		assertThat(request.getUriTemplate()).isEqualTo("/hotels/{id}");
	}

	@Test
	void uriSetsRequestsUrlAndTemplateConsistently() {
		MockHttpServletRequest request = buildRequest(new TestRequestBuilder(HttpMethod.GET).uri("/hotels/{id}", 42)
				.uri(URI.create("/hotels/25")));
		assertThat(request.getRequestURL().toString()).isEqualTo("http://localhost/hotels/25");
		assertThat(request.getUriTemplate()).isNull();
	}

	@Test
	void mergeUriTemplateWhenUriTemplateIsNotSet() {
		TestRequestBuilder parentBuilder = new TestRequestBuilder(HttpMethod.GET).uri("/hotels/{id}", 42);
		TestRequestBuilder builder = new TestRequestBuilder(HttpMethod.POST);
		builder.merge(parentBuilder);

		MockHttpServletRequest request = buildRequest(builder);
		assertThat(request.getUriTemplate()).isEqualTo("/hotels/{id}");
		assertThat(request.getRequestURL().toString()).isEqualTo("http://localhost/hotels/42");
	}


	@Test
	void mergeUriTemplateWhenUriIsSetDoesNotMergeUriTemplate() {
		TestRequestBuilder parentBuilder = new TestRequestBuilder(HttpMethod.GET).uri("/hotels/{id}", 42);
		TestRequestBuilder builder = new TestRequestBuilder(HttpMethod.POST).uri(URI.create("/hotels/35"));
		builder.merge(parentBuilder);

		MockHttpServletRequest request = buildRequest(builder);
		assertThat(request.getUriTemplate()).isNull();
		assertThat(request.getRequestURL().toString()).isEqualTo("http://localhost/hotels/35");
	}

	@Test
	void mergeUriTemplateWhenUriTemplateIsSetDoesNotMergeUriTemplate() {
		TestRequestBuilder parentBuilder = new TestRequestBuilder(HttpMethod.GET).uri("/hotels/{id}", 42);
		TestRequestBuilder builder = new TestRequestBuilder(HttpMethod.POST).uri("/users/{id}", 25);
		builder.merge(parentBuilder);

		MockHttpServletRequest request = buildRequest(builder);
		assertThat(request.getUriTemplate()).isEqualTo("/users/{id}");
		assertThat(request.getRequestURL().toString()).isEqualTo("http://localhost/users/25");
	}

	@Test
	void mergeUriWhenUriIsSetDoesNotOverride() {
		TestRequestBuilder parentBuilder = new TestRequestBuilder(HttpMethod.GET).uri("/test");
		TestRequestBuilder builder = new TestRequestBuilder(HttpMethod.POST).uri("/another");
		builder.merge(parentBuilder);

		MockHttpServletRequest request = buildRequest(builder);
		assertThat(request.getRequestURI()).isEqualTo("/another");
		assertThat(request.getMethod()).isEqualTo(HttpMethod.POST.name());
	}


	private MockHttpServletRequest buildRequest(AbstractMockHttpServletRequestBuilder<?> builder) {
		return builder.buildRequest(this.servletContext);
	}


	private static class TestRequestBuilder extends AbstractMockHttpServletRequestBuilder<TestRequestBuilder> {

		TestRequestBuilder(HttpMethod httpMethod) {
			super(httpMethod);
		}
	}

}
