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

package org.springframework.web.reactive.result.method.annotation;

import java.net.URI;

import org.springframework.context.ApplicationContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;
import org.springframework.web.testfixture.http.server.reactive.bootstrap.AbstractHttpHandlerIntegrationTests;

/**
 * Base class for integration tests with {@code @RequestMapping methods}.
 *
 * @author Rossen Stoyanchev
 */
public abstract class AbstractRequestMappingIntegrationTests extends AbstractHttpHandlerIntegrationTests {

	private ApplicationContext applicationContext;


	@Override
	protected HttpHandler createHttpHandler() {
		this.applicationContext = initApplicationContext();
		return WebHttpHandlerBuilder.applicationContext(this.applicationContext).build();
	}

	protected abstract ApplicationContext initApplicationContext();

	protected ApplicationContext getApplicationContext() {
		return this.applicationContext;
	}



	<T> ResponseEntity<T> performGet(String url, MediaType out, Class<T> type) {
		return getRestClient()
				.get().uri(URI.create(url)).accept(out)
				.retrieve()
				.toEntity(type);
	}

	<T> ResponseEntity<T> performGet(String url, HttpHeaders headers, Class<T> type) {
		return getRestClient()
				.get().uri(URI.create(url)).headers(httpHeaders -> httpHeaders.putAll(headers))
				.retrieve()
				.toEntity(type);
	}

	<T> ResponseEntity<T> performGet(String url, MediaType out, ParameterizedTypeReference<T> type) {
		return getRestClient()
				.get().uri(URI.create(url)).accept(out)
				.retrieve()
				.toEntity(type);
	}

	<T> ResponseEntity<T> performOptions(String url, HttpHeaders headers, Class<T> type) {
		return getRestClient()
				.options().uri(URI.create(url)).headers(httpHeaders -> httpHeaders.putAll(headers))
				.retrieve()
				.toEntity(type);
	}

	<T> ResponseEntity<T> performPost(String url, MediaType in, Object body, MediaType out, Class<T> type) {
		RestClient.RequestBodySpec bodySpec = getRestClient()
				.post().uri(URI.create(url)).accept(out).contentType(in);
		if (body != null) {
			bodySpec.body(body);
		}
		return bodySpec.retrieve().toEntity(type);
	}

	<T> ResponseEntity<T> performPost(String url, HttpHeaders headers, Object body, Class<T> type) {
		RestClient.RequestBodySpec bodySpec = getRestClient()
				.post().uri(URI.create(url)).headers(httpHeaders -> httpHeaders.putAll(headers));
		if (body != null) {
			bodySpec.body(body);
		}
		return bodySpec.retrieve().toEntity(type);
	}

	<T> ResponseEntity<T> performPost(String url, MediaType in, Object body, MediaType out, ParameterizedTypeReference<T> type) {
		return getRestClient()
				.post().uri(URI.create(url)).accept(out).contentType(in).body(body)
				.retrieve()
				.toEntity(type);
	}

}
