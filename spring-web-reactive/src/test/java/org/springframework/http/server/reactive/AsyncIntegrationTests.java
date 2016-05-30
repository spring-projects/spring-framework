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

package org.springframework.http.server.reactive;

import java.net.URI;
import java.time.Duration;

import org.hamcrest.Matchers;
import org.junit.Test;
import reactor.core.publisher.Computations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Timer;

import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.junit.Assert.assertThat;

/**
 * Temporarily does not extend AbstractHttpHandlerIntegrationTests.
 *
 * @author Stephane Maldini
 */
public class AsyncIntegrationTests extends AbstractHttpHandlerIntegrationTests {

	private final Scheduler asyncGroup = Computations.parallel();

	private final DataBufferFactory dataBufferFactory = new DefaultDataBufferFactory();

	@Override
	protected AsyncHandler createHttpHandler() {
		return new AsyncHandler();
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
			return response.writeWith(Flux.just("h", "e", "l", "l", "o")
			                            .useTimer(Timer.global())
			                            .delay(Duration.ofMillis(100))
			                            .publishOn(asyncGroup)
					.collect(dataBufferFactory::allocateBuffer, (buffer, str) -> buffer.write(str.getBytes())));
		}
	}

}
