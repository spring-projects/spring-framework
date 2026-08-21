/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.http.codec.multipart;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.PooledDataBuffer;
import org.springframework.core.testfixture.io.buffer.AbstractLeakCheckingTests;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MultipartParser}.
 *
 * @author Hyunsik Kang
 */
class MultipartParserTests extends AbstractLeakCheckingTests {

	@Test  // gh-37115
	void cancelWithQueuedBodyTokensReleasesBuffers() {
		byte[] boundary = "simple-boundary".getBytes(UTF_8);
		String content = "--simple-boundary\r\nContent-Type: text/plain\r\n\r\n" +
				"a".repeat(1024) + "\r\n--simple-boundary--\r\n";
		byte[] bytes = content.getBytes(UTF_8);
		DataBuffer buffer = this.bufferFactory.allocateBuffer(bytes.length);
		buffer.write(bytes);

		Flux<MultipartParser.Token> tokens = MultipartParser.parse(Flux.just(buffer), boundary, 8192, UTF_8);

		List<MultipartParser.Token> received = new ArrayList<>();
		BaseSubscriber<MultipartParser.Token> subscriber = new BaseSubscriber<>() {
			@Override
			protected void hookOnSubscribe(Subscription subscription) {
				request(1);
			}
			@Override
			protected void hookOnNext(MultipartParser.Token token) {
				received.add(token);
			}
		};
		tokens.subscribe(subscriber);
		// Flux.just delivers synchronously, so by now the parser has emitted the headers
		// token and all body tokens; the body tokens beyond the requested demand of 1 are
		// held in the Flux.create sink queue. Cancelling discards that queue, and the
		// buffers inside the discarded body tokens must be released.
		subscriber.cancel();

		assertThat(received).singleElement().isInstanceOf(MultipartParser.HeadersToken.class);
	}

	@Test  // gh-37115
	void cancelWhileEmittingBodyTokensKeepsEmittedBuffersAllocated() {
		byte[] boundary = "simple-boundary".getBytes(UTF_8);
		String content = "--simple-boundary\r\nContent-Type: text/plain\r\n\r\n" +
				"a".repeat(1024) + "\r\n" +
				"--simple-boundary\r\nContent-Type: text/plain\r\n\r\n" +
				"b".repeat(1024) + "\r\n--simple-boundary--\r\n";
		byte[] bytes = content.getBytes(UTF_8);
		DataBuffer buffer = this.bufferFactory.allocateBuffer(bytes.length);
		buffer.write(bytes);

		Flux<MultipartParser.Token> tokens = MultipartParser.parse(Flux.just(buffer), boundary, 8192, UTF_8);

		List<DataBuffer> receivedBuffers = new ArrayList<>();
		BaseSubscriber<MultipartParser.Token> subscriber = new BaseSubscriber<>() {
			@Override
			protected void hookOnSubscribe(Subscription subscription) {
				request(Long.MAX_VALUE);
			}
			@Override
			protected void hookOnNext(MultipartParser.Token token) {
				if (token instanceof MultipartParser.BodyToken bodyToken) {
					receivedBuffers.add(bodyToken.buffer());
					cancel();
				}
			}
		};
		tokens.subscribe(subscriber);

		// The cancellation above arrives while the parser emits its queued body buffers.
		// Ownership of an emitted buffer belongs to the sink, so the parser must not
		// release it on disposal: with Netty, body buffers are slices of the inbound
		// buffer, and releasing one twice releases the inbound buffer prematurely.
		assertThat(receivedBuffers).isNotEmpty();
		assertThat(receivedBuffers).allSatisfy(received ->
				assertThat(((PooledDataBuffer) received).isAllocated()).isTrue());
		receivedBuffers.forEach(DataBufferUtils::release);
	}

}
