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

package org.springframework.docs.integration.restrestclient.create;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInitializer;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.ApiVersionInserter;
import org.springframework.web.client.RestClient;

public class RestClientCreation {

	void createRestClient() {
		// tag::snippet[]
		RestClient defaultClient = RestClient.create();

		RestClient customClient = RestClient.builder()
				.requestFactory(new HttpComponentsClientHttpRequestFactory())
				.configureMessageConverters(converters -> converters.addCustomConverter(new MyCustomMessageConverter()))
				.baseUrl("https://example.com")
				.defaultUriVariables(Map.of("variable", "foo"))
				.defaultHeader("My-Header", "Foo")
				.defaultCookie("My-Cookie", "Bar")
				.defaultApiVersion("1.2")
				.apiVersionInserter(ApiVersionInserter.useHeader("API-Version"))
				.requestInterceptor(new MyCustomInterceptor())
				.requestInitializer(new MyCustomInitializer())
				.build();
		// end::snippet[]
	}

	private static class MyCustomMessageConverter extends StringHttpMessageConverter {
	}

	private static class MyCustomInterceptor implements ClientHttpRequestInterceptor {

		@Override
		public ClientHttpResponse intercept(HttpRequest request, byte[] body,
				ClientHttpRequestExecution execution) throws IOException {

			return execution.execute(request, body);
		}
	}

	private static class MyCustomInitializer implements ClientHttpRequestInitializer {

		@Override
		public void initialize(ClientHttpRequest request) {
			request.getHeaders().add("My-Header", "My-Value");
		}
	}

}
