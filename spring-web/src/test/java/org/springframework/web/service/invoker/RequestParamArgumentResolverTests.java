/*
 * Copyright 2002-2022 the original author or authors.
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
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.util.UriComponentsBuilder;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;


/**
 * Unit tests for {@link RequestParamArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 */
public class RequestParamArgumentResolverTests {

	private final TestHttpClientAdapter clientAdapter = new TestHttpClientAdapter();

	private final Service service = this.clientAdapter.createService(Service.class);


	@Test
	void formData() {
		this.service.postForm("value 1", "value 2");

		Object body = this.clientAdapter.getRequestValues().getBodyValue();
		assertThat(body).isNotNull().isInstanceOf(byte[].class);
		assertThat(new String((byte[]) body, UTF_8)).isEqualTo("param1=value+1&param2=value+2");
	}

	@Test
	void uriTemplate() {
		this.service.search("1st value", Arrays.asList("2nd value A", "2nd value B"));

		HttpRequestValues requestValues = this.clientAdapter.getRequestValues();

		assertThat(requestValues.getUriTemplate())
				.isEqualTo("/path?" +
						"{queryParam0}={queryParam0[0]}&" +
						"{queryParam1}={queryParam1[0]}&" +
						"{queryParam1}={queryParam1[1]}");

		assertThat(requestValues.getUriVariables())
				.containsOnlyKeys("queryParam0", "queryParam1", "queryParam0[0]", "queryParam1[0]", "queryParam1[1]")
				.containsEntry("queryParam0", "param1")
				.containsEntry("queryParam1", "param2")
				.containsEntry("queryParam0[0]", "1st value")
				.containsEntry("queryParam1[0]", "2nd value A")
				.containsEntry("queryParam1[1]", "2nd value B");

		URI uri = UriComponentsBuilder.fromUriString(requestValues.getUriTemplate())
				.encode().build(requestValues.getUriVariables());

		assertThat(uri.toString())
				.isEqualTo("/path?param1=1st%20value&param2=2nd%20value%20A&param2=2nd%20value%20B");
	}

	@Test
	void uri() {
		URI baseUrl = URI.create("http://localhost:8080/path");
		this.service.searchWithDynamicUri(baseUrl, "1st value", Arrays.asList("2nd value A", "2nd value B"));

		assertThat(this.clientAdapter.getRequestValues().getUri().toString())
				.isEqualTo(baseUrl + "?param1=1st%20value&param2=2nd%20value%20A&param2=2nd%20value%20B");
	}


	private interface Service {

		@PostExchange(contentType = "application/x-www-form-urlencoded")
		void postForm(@RequestParam String param1, @RequestParam String param2);

		@GetExchange("/path")
		void search(@RequestParam String param1, @RequestParam List<String> param2);

		@GetExchange
		void searchWithDynamicUri(URI uri, @RequestParam String param1, @RequestParam List<String> param2);
	}

}
