/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.AbstractHttpHandlerIntegrationTests;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

import static org.springframework.http.RequestEntity.get;

/**
 *
 * @author Rossen Stoyanchev
 */
public abstract class AbstractRequestMappingIntegrationTests extends AbstractHttpHandlerIntegrationTests {

	private ApplicationContext applicationContext;

	private RestTemplate restTemplate = new RestTemplate();


	@Override
	protected HttpHandler createHttpHandler() {
		this.applicationContext = initApplicationContext();
		return WebHttpHandlerBuilder
				.webHandler(new DispatcherHandler(this.applicationContext))
				.build();
	}

	protected abstract ApplicationContext initApplicationContext();


	ApplicationContext getApplicationContext() {
		return this.applicationContext;
	}


	<T> ResponseEntity<T> performGet(String url, MediaType out,
			Class<T> type) throws Exception {

		return this.restTemplate.exchange(prepareGet(url, out), type);
	}

	<T> ResponseEntity<T> performGet(String url, MediaType out,
			ParameterizedTypeReference<T> type) throws Exception {

		return this.restTemplate.exchange(prepareGet(url, out), type);
	}

	<T> ResponseEntity<T> performPost(String url, MediaType in, Object body, MediaType out,
			Class<T> type) throws Exception {

		return  this.restTemplate.exchange(preparePost(url, in, body, out), type);
	}

	<T> ResponseEntity<T> performPost(String url, MediaType in, Object body,
			MediaType out, ParameterizedTypeReference<T> type) throws Exception {

		return this.restTemplate.exchange(preparePost(url, in, body, out), type);
	}

	private RequestEntity<Void> prepareGet(String url, MediaType accept) throws Exception {
		URI uri = new URI("http://localhost:" + this.port + url);
		return (accept != null ? get(uri).accept(accept).build() : get(uri).build());
	}

	private RequestEntity<?> preparePost(String url, MediaType in, Object body, MediaType out) throws Exception {
		URI uri = new URI("http://localhost:" + this.port + url);
		return (out != null ?
				RequestEntity.post(uri).contentType(in).accept(out).body(body) :
				RequestEntity.post(uri).contentType(in).body(body));
	}
}
