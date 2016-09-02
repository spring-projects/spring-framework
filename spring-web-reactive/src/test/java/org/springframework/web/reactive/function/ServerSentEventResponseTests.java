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

package org.springframework.web.reactive.function;

import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.server.reactive.MockServerHttpRequest;
import org.springframework.http.server.reactive.MockServerHttpResponse;
import org.springframework.tests.TestSubscriber;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.session.MockWebSessionManager;

import static org.junit.Assert.assertEquals;

/**
 * @author Arjen Poutsma
 */
public class ServerSentEventResponseTests {

	private final ServerSentEvent<String> sse =
			ServerSentEvent.<String>builder().data("42").build();

	private final Publisher<ServerSentEvent<String>> body = Mono.just(sse);

	private final ServerSentEventResponse<ServerSentEvent<String>> sseResponse =
			ServerSentEventResponse.fromSseEvents(200, new HttpHeaders(), body);

	@Test
	public void body() throws Exception {
		assertEquals(body, sseResponse.body());
	}

	@Test
	public void writeTo() throws Exception {
		MockServerHttpRequest request = new MockServerHttpRequest(HttpMethod.GET, "http://localhost");
		MockServerHttpResponse response = new MockServerHttpResponse();
		ServerWebExchange exchange = new DefaultServerWebExchange(request, response, new MockWebSessionManager());

		sseResponse.writeTo(exchange);
		Publisher<Publisher<DataBuffer>> result = response.getBodyWithFlush();
		TestSubscriber.subscribe(result).
				assertNoError().
				assertValuesWith(publisher -> {
					TestSubscriber.subscribe(publisher).assertNoError();

				});
	}

}