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

package org.springframework.web.client.reactive;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.springframework.web.client.reactive.ClientWebRequestBuilders.*;
import static org.springframework.web.client.reactive.ResponseExtractors.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.tests.TestSubscriber;
import org.springframework.web.client.reactive.test.MockClientHttpRequest;
import org.springframework.web.client.reactive.test.MockClientHttpResponse;

/**
 * @author Brian Clozel
 */
public class ClientHttpRequestInterceptorTests {

	private MockClientHttpRequest mockRequest;

	private MockClientHttpResponse mockResponse;

	private MockClientHttpConnector mockClientHttpConnector;

	private WebClient webClient;


	@Before
	public void setUp() throws Exception {
		this.mockClientHttpConnector = new MockClientHttpConnector();
		this.webClient = new WebClient(this.mockClientHttpConnector);
		this.mockResponse = new MockClientHttpResponse();
		this.mockResponse.setStatus(HttpStatus.OK);
		this.mockResponse.getHeaders().setContentType(MediaType.TEXT_PLAIN);
		this.mockResponse.setBody("Spring Framework");
	}

	@Test
	public void shouldExecuteInterceptors() throws Exception {
		List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
		interceptors.add(new NoOpInterceptor());
		interceptors.add(new NoOpInterceptor());
		interceptors.add(new NoOpInterceptor());
		this.webClient.setInterceptors(interceptors);

		Mono<String> result = this.webClient.perform(get("http://example.org/resource"))
				.extract(body(String.class));

		TestSubscriber.subscribe(result)
				.assertNoError()
				.assertValues("Spring Framework")
				.assertComplete();
		interceptors.stream().forEach(interceptor -> {
			Assert.assertTrue(((NoOpInterceptor) interceptor).invoked);
		});
	}

	@Test
	public void shouldChangeRequest() throws Exception {
		ClientHttpRequestInterceptor interceptor = new ClientHttpRequestInterceptor() {
			@Override
			public Mono<ClientHttpResponse> intercept(HttpMethod method, URI uri,
					ClientHttpRequestInterceptionChain interception) {

				return interception.intercept(HttpMethod.POST, URI.create("http://example.org/other"),
						(request) -> {
							request.getHeaders().set("X-Custom", "Spring Framework");
						});
			}
		};
		this.webClient.setInterceptors(Collections.singletonList(interceptor));

		Mono<String> result = this.webClient.perform(get("http://example.org/resource"))
				.extract(body(String.class));

		TestSubscriber.subscribe(result)
				.assertNoError()
				.assertValues("Spring Framework")
				.assertComplete();

		assertThat(this.mockRequest.getMethod(), is(HttpMethod.POST));
		assertThat(this.mockRequest.getURI().toString(), is("http://example.org/other"));
		assertThat(this.mockRequest.getHeaders().getFirst("X-Custom"), is("Spring Framework"));
	}

	@Test
	public void shouldShortCircuitConnector() throws Exception {

		MockClientHttpResponse otherResponse = new MockClientHttpResponse();
		otherResponse.setStatus(HttpStatus.OK);
		otherResponse.setBody("Other content");

		List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
		interceptors.add((method, uri, interception) -> Mono.just(otherResponse));
		interceptors.add(new NoOpInterceptor());
		this.webClient.setInterceptors(interceptors);

		Mono<String> result = this.webClient.perform(get("http://example.org/resource"))
				.extract(body(String.class));

		TestSubscriber.subscribe(result)
				.assertNoError()
				.assertValues("Other content")
				.assertComplete();

		assertFalse(((NoOpInterceptor) interceptors.get(1)).invoked);
	}

	private class MockClientHttpConnector implements ClientHttpConnector {

		@Override
		public Mono<ClientHttpResponse> connect(HttpMethod method, URI uri,
				Function<? super ClientHttpRequest, Mono<Void>> requestCallback) {

			mockRequest = new MockClientHttpRequest(method, uri);
			return requestCallback.apply(mockRequest).then(Mono.just(mockResponse));
		}
	}


	private static class NoOpInterceptor implements ClientHttpRequestInterceptor {

		public boolean invoked = false;

		@Override
		public Mono<ClientHttpResponse> intercept(HttpMethod method, URI uri,
				ClientHttpRequestInterceptionChain interception) {

			this.invoked = true;
			return interception.intercept(method, uri, (request) -> { });
		}
	}
}
