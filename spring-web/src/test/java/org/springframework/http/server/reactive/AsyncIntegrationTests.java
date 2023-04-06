/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.http.server.reactive;

import java.net.URI;
import java.time.Duration;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.testfixture.http.server.reactive.bootstrap.AbstractHttpHandlerIntegrationTests;
import org.springframework.web.testfixture.http.server.reactive.bootstrap.HttpServer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Stephane Maldini
 * @since 5.0
 */
class AsyncIntegrationTests extends AbstractHttpHandlerIntegrationTests {

	private final Scheduler asyncGroup = Schedulers.parallel();


	@Override
	protected AsyncHandler createHttpHandler() {
		return new AsyncHandler();
	}

	@ParameterizedHttpServerTest
	void basicTest(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		URI url = URI.create("http://localhost:" + port);
		@SuppressWarnings("resource")
		ResponseEntity<String> response = new RestTemplate().exchange(RequestEntity.get(url).build(), String.class);

		assertThat(response.getBody()).isEqualTo("hello");
	}


	private class AsyncHandler implements HttpHandler {

		@Override
		@SuppressWarnings("deprecation")
		public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
			return response.writeWith(Flux.just("h", "e", "l", "l", "o")
										.delayElements(Duration.ofMillis(100))
										.publishOn(asyncGroup)
					.collect(DefaultDataBufferFactory.sharedInstance::allocateBuffer,
							(buffer, str) -> buffer.write(str.getBytes())));
		}
	}

}
