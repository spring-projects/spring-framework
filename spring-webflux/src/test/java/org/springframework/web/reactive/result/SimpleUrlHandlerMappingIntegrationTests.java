/*
 * Copyright 2002-2018 the original author or authors.
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
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.AbstractHttpHandlerIntegrationTests;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.server.WebHandler;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;
import org.springframework.web.server.handler.ResponseStatusExceptionHandler;

import static org.junit.Assert.*;

/**
 * Integration tests with requests mapped via
 * {@link SimpleUrlHandlerMapping} to plain {@link WebHandler}s.
 *
 * @author Rossen Stoyanchev
 */
public class SimpleUrlHandlerMappingIntegrationTests extends AbstractHttpHandlerIntegrationTests {

	@Override
	protected HttpHandler createHttpHandler() {
		AnnotationConfigApplicationContext wac = new AnnotationConfigApplicationContext();
		wac.register(WebConfig.class);
		wac.refresh();

		return WebHttpHandlerBuilder.webHandler(new DispatcherHandler(wac))
				.exceptionHandler(new ResponseStatusExceptionHandler())
				.build();
	}


	@Test
	public void testRequestToFooHandler() throws Exception {
		URI url = new URI("http://localhost:" + this.port + "/foo");
		RequestEntity<Void> request = RequestEntity.get(url).build();
		ResponseEntity<byte[]> response = new RestTemplate().exchange(request, byte[].class);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertArrayEquals("foo".getBytes("UTF-8"), response.getBody());
	}

	@Test
	public void testRequestToBarHandler() throws Exception {
		URI url = new URI("http://localhost:" + this.port + "/bar");
		RequestEntity<Void> request = RequestEntity.get(url).build();
		ResponseEntity<byte[]> response = new RestTemplate().exchange(request, byte[].class);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertArrayEquals("bar".getBytes("UTF-8"), response.getBody());
	}

	@Test
	public void testRequestToHeaderSettingHandler() throws Exception {
		URI url = new URI("http://localhost:" + this.port + "/header");
		RequestEntity<Void> request = RequestEntity.get(url).build();
		ResponseEntity<byte[]> response = new RestTemplate().exchange(request, byte[].class);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals("bar", response.getHeaders().getFirst("foo"));
	}

	@Test
	public void testHandlerNotFound() throws Exception {
		URI url = new URI("http://localhost:" + this.port + "/oops");
		RequestEntity<Void> request = RequestEntity.get(url).build();
		try {
			new RestTemplate().exchange(request, byte[].class);
		}
		catch (HttpClientErrorException ex) {
			assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
		}
	}

	private static DataBuffer asDataBuffer(String text) {
		DefaultDataBuffer buffer = new DefaultDataBufferFactory().allocateBuffer();
		return buffer.write(text.getBytes(StandardCharsets.UTF_8));
	}


	@Configuration
	static class WebConfig {

		@Bean
		public SimpleUrlHandlerMapping handlerMapping() {
			return new SimpleUrlHandlerMapping() {
				{
					Map<String, Object> map = new HashMap<>();
					map.put("/foo", (WebHandler) exchange ->
							exchange.getResponse().writeWith(Flux.just(asDataBuffer("foo"))));
					map.put("/bar", (WebHandler) exchange ->
							exchange.getResponse().writeWith(Flux.just(asDataBuffer("bar"))));
					map.put("/header", (WebHandler) exchange -> {
						exchange.getResponse().getHeaders().add("foo", "bar");
						return Mono.empty();
					});
					setUrlMap(map);
				}
			};
		}

		@Bean
		public SimpleHandlerAdapter handlerAdapter() {
			return new SimpleHandlerAdapter();
		}
	}

}
