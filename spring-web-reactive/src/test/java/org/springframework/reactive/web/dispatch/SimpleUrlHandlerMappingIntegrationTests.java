/*
 * Copyright 2002-2015 the original author or authors.
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
package org.springframework.reactive.web.dispatch;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.rx.Streams;

import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.reactive.web.dispatch.handler.HttpHandlerAdapter;
import org.springframework.reactive.web.dispatch.handler.SimpleUrlHandlerMapping;
import org.springframework.reactive.web.http.AbstractHttpHandlerIntegrationTests;
import org.springframework.reactive.web.http.HttpHandler;
import org.springframework.reactive.web.http.ServerHttpRequest;
import org.springframework.reactive.web.http.ServerHttpResponse;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.support.StaticWebApplicationContext;

import static org.junit.Assert.assertArrayEquals;


/**
 * @author Rossen Stoyanchev
 */
public class SimpleUrlHandlerMappingIntegrationTests extends AbstractHttpHandlerIntegrationTests {

	private static final Charset CHARSET = Charset.forName("UTF-8");


	@Override
	protected HttpHandler createHttpHandler() {

		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.registerSingleton("hm", TestHandlerMapping.class);
		wac.registerSingleton("ha", HttpHandlerAdapter.class);
		wac.refresh();

		DispatcherHandler dispatcherHandler = new DispatcherHandler();
		dispatcherHandler.initStrategies(wac);
		return dispatcherHandler;
	}

	@Test
	public void testFoo() throws Exception {

		RestTemplate restTemplate = new RestTemplate();

		URI url = new URI("http://localhost:" + port + "/foo");
		RequestEntity<Void> request = RequestEntity.get(url).build();
		ResponseEntity<byte[]> response = restTemplate.exchange(request, byte[].class);

		assertArrayEquals("foo".getBytes(CHARSET), response.getBody());
	}

	@Test
	public void testBar() throws Exception {

		RestTemplate restTemplate = new RestTemplate();

		URI url = new URI("http://localhost:" + port + "/bar");
		RequestEntity<Void> request = RequestEntity.get(url).build();
		ResponseEntity<byte[]> response = restTemplate.exchange(request, byte[].class);

		assertArrayEquals("bar".getBytes(CHARSET), response.getBody());
	}


	private static class TestHandlerMapping extends SimpleUrlHandlerMapping {

		public TestHandlerMapping() {
			Map<String, Object> map = new HashMap<>();
			map.put("/foo", new FooHandler());
			map.put("/bar", new BarHandler());
			setHandlers(map);
		}
	}

	private static class FooHandler implements HttpHandler {

		@Override
		public Publisher<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
			return response.writeWith(Streams.just("foo".getBytes(CHARSET)));
		}
	}

	private static class BarHandler implements HttpHandler {

		@Override
		public Publisher<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
			return response.writeWith(Streams.just("bar".getBytes(CHARSET)));
		}
	}

}
