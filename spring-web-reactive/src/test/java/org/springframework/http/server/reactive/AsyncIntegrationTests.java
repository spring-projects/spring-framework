/*
 * Copyright (c) 2011-2016 Pivotal Software Inc, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.http.server.reactive;

import java.net.URI;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import reactor.Mono;
import reactor.Processors;
import reactor.Timers;
import reactor.core.processor.ProcessorGroup;
import reactor.io.buffer.Buffer;
import reactor.rx.Stream;

import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.boot.HttpServer;
import org.springframework.http.server.reactive.boot.ReactorHttpServer;
import org.springframework.http.server.reactive.boot.RxNettyHttpServer;
import org.springframework.http.server.reactive.boot.UndertowHttpServer;
import org.springframework.util.SocketUtils;
import org.springframework.web.client.RestTemplate;

import static org.junit.Assert.assertThat;

/**
 * Temporarily does not extend AbstractHttpHandlerIntegrationTests.
 *
 * @author Stephane Maldini
 */
@RunWith(Parameterized.class)
public class AsyncIntegrationTests {

	private final ProcessorGroup asyncGroup = Processors.asyncGroup();

	protected int port;

	@Parameterized.Parameter(0)
	public HttpServer server;

	private AsyncHandler asyncHandler;

	@Parameterized.Parameters(name = "server [{0}]")
	public static Object[][] arguments() {
		return new Object[][]{
				//{new JettyHttpServer()},
				{new RxNettyHttpServer()},
				{new ReactorHttpServer()},
				//{new TomcatHttpServer()},
				{new UndertowHttpServer()}
		};
	}

	@Before
	public void setup() throws Exception {
		this.port = SocketUtils.findAvailableTcpPort();
		this.server.setPort(this.port);
		this.server.setHandler(createHttpHandler());
		this.server.afterPropertiesSet();
		this.server.start();
	}

	protected HttpHandler createHttpHandler() {
		this.asyncHandler = new AsyncHandler();
		return this.asyncHandler;
	}

	@After
	public void tearDown() throws Exception {
		this.server.stop();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void basicTest() throws Exception {
		URI url = new URI("http://localhost:" + port);
		ResponseEntity<String> response = new RestTemplate().exchange(RequestEntity.get(url)
		                                                                             .build(), String.class);

		assertThat(response.getBody(), Matchers.equalTo("hello"));
	}

	private class AsyncHandler implements HttpHandler {

		@Override
		public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
			return response.setBody(Stream.just("h", "e", "l", "l", "o")
			                              .timer(Timers.global())
			                              .throttleRequest(100)
			                              .dispatchOn(asyncGroup)
			                              .collect(Buffer::new, Buffer::append)
			                              .doOnSuccess(Buffer::flip)
			                              .map(Buffer::byteBuffer)
			);
		}
	}

}
