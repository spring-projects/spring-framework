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

package org.springframework.web.reactive.result;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.AbstractHttpHandlerIntegrationTests;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.reactive.ResponseStatusExceptionHandler;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebHandler;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;


/**
 * Integration tests with requests mapped to plain {@link WebHandler}s.
 *
 * @author Rossen Stoyanchev
 */
public class WebHandlerIntegrationTests extends AbstractHttpHandlerIntegrationTests {

	private static final Charset UTF_8 = Charset.forName("UTF-8");


	@Override
	protected HttpHandler createHttpHandler() {

		StaticApplicationContext wac = new StaticApplicationContext();
		wac.registerSingleton("handlerMapping", TestSimpleUrlHandlerMapping.class);
		wac.registerSingleton("handlerAdapter", SimpleHandlerAdapter.class);
		wac.registerSingleton("resultHandler", SimpleResultHandler.class);
		wac.refresh();

		DispatcherHandler dispatcherHandler = new DispatcherHandler();
		dispatcherHandler.setApplicationContext(wac);

		return WebHttpHandlerBuilder.webHandler(dispatcherHandler)
				.exceptionHandlers(new ResponseStatusExceptionHandler())
				.build();
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


	private static class TestSimpleUrlHandlerMapping extends SimpleUrlHandlerMapping {

		public TestSimpleUrlHandlerMapping() {
			Map<String, Object> map = new HashMap<>();
			map.put("/foo", new FooHandler());
			map.put("/bar", new BarHandler());
			map.put("/header", new HeaderSettingHandler());
			setUrlMap(map);
		}
	}

	private static DataBuffer asDataBuffer(String text) {
		return new DefaultDataBufferFactory().allocateBuffer()
				.write(text.getBytes(StandardCharsets.UTF_8));
	}

	private static class FooHandler implements WebHandler {

		@Override
		public Mono<Void> handle(ServerWebExchange exchange) {
			DataBuffer buffer = asDataBuffer("foo");
			return exchange.getResponse().setBody(Flux.just(buffer));
		}
	}

	private static class BarHandler implements WebHandler {

		@Override
		public Mono<Void> handle(ServerWebExchange exchange) {
			DataBuffer buffer = asDataBuffer("bar");
			return exchange.getResponse().setBody(Flux.just(buffer));
		}
	}

	private static class HeaderSettingHandler implements WebHandler {

		@Override
		public Mono<Void> handle(ServerWebExchange exchange) {
			exchange.getResponse().getHeaders().add("foo", "bar");
			return Mono.empty();
		}
	}

}
