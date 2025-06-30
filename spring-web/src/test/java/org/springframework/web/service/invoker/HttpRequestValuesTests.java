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

package org.springframework.web.service.invoker;

import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HttpRequestValues}.
 *
 * @author Rossen Stoyanchev
 */
class HttpRequestValuesTests {

	@Test
	void defaultUri() {
		HttpRequestValues requestValues = HttpRequestValues.builder().setHttpMethod(HttpMethod.GET).build();

		assertThat(requestValues.getUri()).isNull();
		assertThat(requestValues.getUriTemplate()).isEmpty();
		assertThat(requestValues.getUriBuilderFactory()).isNull();
	}

	@ParameterizedTest
	@ValueSource(strings = {"POST", "PUT", "PATCH"})
	@SuppressWarnings("unchecked")
	void formData(String httpMethod) {

		HttpRequestValues requestValues = HttpRequestValues.builder().setHttpMethod(HttpMethod.valueOf(httpMethod))
				.setContentType(MediaType.APPLICATION_FORM_URLENCODED)
				.addRequestParameter("param1", "1st value")
				.addRequestParameter("param2", "2nd value A", "2nd value B")
				.build();

		Object body = requestValues.getBodyValue();
		assertThat((MultiValueMap<String, String>) body).hasSize(2)
				.containsEntry("param1", List.of("1st value"))
				.containsEntry("param2", List.of("2nd value A", "2nd value B"));
	}

	@Test
	void queryParamsWithUriTemplate() {

		HttpRequestValues requestValues = HttpRequestValues.builder().setHttpMethod(HttpMethod.POST)
				.setUriTemplate("/path")
				.addRequestParameter("param1", "1st value")
				.addRequestParameter("param2", "2nd value A", "2nd value B")
				.build();

		String uriTemplate = requestValues.getUriTemplate();
		assertThat(uriTemplate).isNotNull();

		assertThat(uriTemplate)
				.isEqualTo("/path?" +
						"{queryParam-param1}={queryParam-param1[0]}&" +
						"{queryParam-param2}={queryParam-param2[0]}&" +
						"{queryParam-param2}={queryParam-param2[1]}");

		assertThat(requestValues.getUriVariables())
				.containsOnlyKeys(
						"queryParam-param1", "queryParam-param2", "queryParam-param1[0]",
						"queryParam-param2[0]", "queryParam-param2[1]")
				.containsEntry("queryParam-param1", "param1")
				.containsEntry("queryParam-param2", "param2")
				.containsEntry("queryParam-param1[0]", "1st value")
				.containsEntry("queryParam-param2[0]", "2nd value A")
				.containsEntry("queryParam-param2[1]", "2nd value B");

		URI uri = UriComponentsBuilder.fromUriString(uriTemplate)
				.encode()
				.build(requestValues.getUriVariables());

		assertThat(uri.toString())
				.isEqualTo("/path?param1=1st%20value&param2=2nd%20value%20A&param2=2nd%20value%20B");
	}

	@Test // gh-34364
	void queryParamWithSemicolon() {
		HttpRequestValues requestValues = HttpRequestValues.builder().setHttpMethod(HttpMethod.POST)
				.setUriTemplate("/path")
				.addRequestParameter("userId:eq", "test value")
				.build();

		String uriTemplate = requestValues.getUriTemplate();
		assertThat(uriTemplate).isEqualTo("/path?{queryParam-userId%3Aeq}={queryParam-userId%3Aeq[0]}");

		URI uri = UriComponentsBuilder.fromUriString(uriTemplate)
				.encode()
				.build(requestValues.getUriVariables());

		assertThat(uri.toString())
				.isEqualTo("/path?userId%3Aeq=test%20value");
	}

	@Test
	void queryParamsWithPreparedUri() {

		URI uri = URI.create("/my%20path");

		HttpRequestValues requestValues = HttpRequestValues.builder().setHttpMethod(HttpMethod.POST)
				.setUri(uri)
				.addRequestParameter("param1", "1st value")
				.addRequestParameter("param2", "2nd value A", "2nd value B")
				.build();

		assertThat(requestValues.getUri().toString())
				.isEqualTo("/my%20path?param1=1st%20value&param2=2nd%20value%20A&param2=2nd%20value%20B");
	}

	@Test
	void requestPart() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("foo", "bar");
		HttpEntity<String> entity = new HttpEntity<>("body", headers);

		HttpRequestValues requestValues = HttpRequestValues.builder()
				.addRequestPart("form field", "form value")
				.addRequestPart("entity", entity)
				.build();

		@SuppressWarnings("unchecked")
		MultiValueMap<String, Object> map = (MultiValueMap<String, Object>) requestValues.getBodyValue();
		assertThat(map).hasSize(2);
		assertThat(map.getFirst("form field")).isEqualTo("form value");
		assertThat(map.getFirst("entity")).isEqualTo(entity);
	}

	@Test
	void requestPartAndRequestParam() {

		HttpRequestValues requestValues = HttpRequestValues.builder()
				.setUriTemplate("/path")
				.addRequestPart("form field", "form value")
				.addRequestParameter("query param", "query value")
				.build();

		String uriTemplate = requestValues.getUriTemplate();
		assertThat(uriTemplate).isNotNull();

		assertThat(uriTemplate).isEqualTo("/path?{queryParam-query param}={queryParam-query param[0]}");

		URI uri = UriComponentsBuilder.fromUriString(uriTemplate)
				.encode()
				.build(requestValues.getUriVariables());
		assertThat(uri.toString())
				.isEqualTo("/path?query%20param=query%20value");

		@SuppressWarnings("unchecked")
		MultiValueMap<String, Object> map = (MultiValueMap<String, Object>) requestValues.getBodyValue();
		assertThat(map).hasSize(1);
		assertThat(map.getFirst("form field")).isEqualTo("form value");
	}

}
