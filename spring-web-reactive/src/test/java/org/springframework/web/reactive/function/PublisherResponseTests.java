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

import java.util.Collections;

import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.codec.EncoderHttpMessageWriter;
import org.springframework.http.server.reactive.MockServerHttpRequest;
import org.springframework.http.server.reactive.MockServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.session.MockWebSessionManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Arjen Poutsma
 */
public class PublisherResponseTests {

	private final Publisher<String> publisher = Flux.just("foo", "bar");

	private final PublisherResponse<String> publisherResponse =
			new PublisherResponse<>(200, new HttpHeaders(), publisher, String.class);

	@Test
	public void body() throws Exception {
		assertEquals(publisher, publisherResponse.body());
	}

	@Test
	public void writeTo() throws Exception {
		MockServerHttpRequest request = new MockServerHttpRequest(HttpMethod.GET, "http://localhost");
		MockServerHttpResponse response = new MockServerHttpResponse();
		ServerWebExchange exchange = new DefaultServerWebExchange(request, response, new MockWebSessionManager());
		exchange.getAttributes().put(Router.HTTP_MESSAGE_WRITERS_ATTRIBUTE, Collections
				.singleton(new EncoderHttpMessageWriter<CharSequence>(new CharSequenceEncoder())).stream());


		publisherResponse.writeTo(exchange).block();
		assertNotNull(response.getBody());
	}

}