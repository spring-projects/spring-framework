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

package org.springframework.web.reactive.handler;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import reactor.Mono;
import reactor.io.buffer.Buffer;
import reactor.rx.Streams;

import org.springframework.context.support.StaticApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.AbstractHttpHandlerIntegrationTests;
import org.springframework.http.server.reactive.ErrorHandlingHttpHandler;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.reactive.ResponseStatusExceptionHandler;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;


/**
 * @author Rossen Stoyanchev
 */
public class SimpleUrlHandlerMappingIntegrationTests extends AbstractHttpHandlerIntegrationTests {

	private static final Charset UTF_8 = Charset.forName("UTF-8");


	@Override
	protected HttpHandler createHttpHandler() {

		StaticApplicationContext wac = new StaticApplicationContext();
		wac.registerSingleton("hm", TestHandlerMapping.class);
		wac.registerSingleton("ha", HttpHandlerAdapter.class);
		wac.registerSingleton("rh", SimpleHandlerResultHandler.class);
		wac.refresh();

		DispatcherHandler dispatcherHandler = new DispatcherHandler();
		dispatcherHandler.setApplicationContext(wac);
		return new ErrorHandlingHttpHandler(dispatcherHandler, new ResponseStatusExceptionHandler());
	}

	@Test
	public void testFooHandler() throws Exception {

		RestTemplate restTemplate = new RestTemplate();

		URI url = new URI("http://localhost:" + port + "/foo");
		RequestEntity<Void> request = RequestEntity.get(url).build();
		ResponseEntity<byte[]> response = restTemplate.exchange(request, byte[].class);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertArrayEquals("foo".getBytes(UTF_8), response.getBody());
	}

	@Test
	public void testBarHandler() throws Exception {

		RestTemplate restTemplate = new RestTemplate();

		URI url = new URI("http://localhost:" + port + "/bar");
		RequestEntity<Void> request = RequestEntity.get(url).build();
		ResponseEntity<byte[]> response = restTemplate.exchange(request, byte[].class);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertArrayEquals("bar".getBytes(UTF_8), response.getBody());
	}

	@Test
	public void testHeaderSettingHandler() throws Exception {

		RestTemplate restTemplate = new RestTemplate();

		URI url = new URI("http://localhost:" + port + "/header");
		RequestEntity<Void> request = RequestEntity.get(url).build();
		ResponseEntity<byte[]> response = restTemplate.exchange(request, byte[].class);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals("bar", response.getHeaders().getFirst("foo"));
	}

	@Test
	public void testNotFound() throws Exception {

		RestTemplate restTemplate = new RestTemplate();

		URI url = new URI("http://localhost:" + port + "/oops");
		RequestEntity<Void> request = RequestEntity.get(url).build();
		try {
			restTemplate.exchange(request, byte[].class);
		}
		catch (HttpClientErrorException ex) {
			assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
		}
	}


	private static class TestHandlerMapping extends SimpleUrlHandlerMapping {

		public TestHandlerMapping() {
			Map<String, Object> map = new HashMap<>();
			map.put("/foo", new FooHandler());
			map.put("/bar", new BarHandler());
			map.put("/header", new HeaderSettingHandler());
			setHandlers(map);
		}
	}

	private static class FooHandler implements HttpHandler {

		@Override
		public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
			return response.setBody(Streams.just(Buffer.wrap("foo").byteBuffer()));
		}
	}

	private static class BarHandler implements HttpHandler {

		@Override
		public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
			return response.setBody(Streams.just(Buffer.wrap("bar").byteBuffer()));
		}
	}

	private static class HeaderSettingHandler implements HttpHandler {

		@Override
		public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
			response.getHeaders().add("foo", "bar");
			return Mono.empty();
		}
	}

}
