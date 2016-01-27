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

import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferAllocator;

import static org.junit.Assert.assertEquals;

/**
 * @author Arjen Poutsma
 */
public class RandomHandler implements HttpHandler {

	private static final Log logger = LogFactory.getLog(RandomHandler.class);

	public static final int RESPONSE_SIZE = 4096 * 3;

	private final Random rnd = new Random();

	@Override
	public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {

		request.getBody().subscribe(new Subscriber<DataBuffer>() {
			private Subscription s;

			private int requestSize = 0;

			@Override
			public void onSubscribe(Subscription s) {
				this.s = s;
				s.request(Long.MAX_VALUE);
			}

			@Override
			public void onNext(DataBuffer bytes) {
				requestSize += bytes.readableByteCount();
			}

			@Override
			public void onError(Throwable t) {
				logger.error(t);

			}

			@Override
			public void onComplete() {
				logger.debug("Complete");
				assertEquals(RandomHandlerIntegrationTests.REQUEST_SIZE, requestSize);
			}
		});

		response.getHeaders().setContentLength(RESPONSE_SIZE);
		byte[] randomBytes = randomBytes();
		DataBuffer buffer =
				new DefaultDataBufferAllocator().allocateBuffer(randomBytes.length);
		buffer.write(randomBytes);
		return response.setBody(Mono.just(buffer));
	}

	private byte[] randomBytes() {
		byte[] buffer = new byte[RESPONSE_SIZE];
		rnd.nextBytes(buffer);
		return buffer;
	}

}
