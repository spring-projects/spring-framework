/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.codec.multipart;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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


	@Test  // gh-XXXXX
	void boundaryDelimiterSplitAcrossBuffers() {
		byte[] boundary = "simple-boundary".getBytes(UTF_8);
		String content = "--simple-boundary\r\nContent-Type: text/plain\r\n\r\n" +
				"one\r\n--simple-boundary\r\nContent-Type: text/plain\r\n\r\n" +
				"second\r\n--simple-boundary--\r\n";
		byte[] bytes = content.getBytes(UTF_8);

		// Split after the CR of each boundary's trailing CRLF (and inside the final "--"),
		// so that the boundary delimiter spans two buffers.
		int firstSplit = content.indexOf("--simple-boundary\r") + "--simple-boundary\r".length();
		int secondSplit = content.indexOf("--simple-boundary\r", firstSplit) + "--simple-boundary\r".length();
		int thirdSplit = content.indexOf("--simple-boundary--") + "--simple-boundary-".length();

		List<DataBuffer> buffers = List.of(
				createBuffer(bytes, 0, firstSplit),
				createBuffer(bytes, firstSplit, secondSplit),
				createBuffer(bytes, secondSplit, thirdSplit),
				createBuffer(bytes, thirdSplit, bytes.length));

		List<MultipartParser.Token> tokens = MultipartParser
				.parse(Flux.fromIterable(buffers), boundary, 8192, UTF_8)
				.collectList()
				.block(Duration.ofSeconds(5));

		assertThat(tokens).isNotNull();
		String body = tokens.stream()
				.filter(MultipartParser.BodyToken.class::isInstance)
				.map(MultipartParser.BodyToken.class::cast)
				.map(MultipartParser.BodyToken::buffer)
				.map(buffer -> {
					String result = buffer.toString(UTF_8);
					DataBufferUtils.release(buffer);
					return result;
				})
				.collect(Collectors.joining());
		assertThat(body).isEqualTo("onesecond");
	}

	private DataBuffer createBuffer(byte[] bytes, int from, int to) {
		DataBuffer buffer = this.bufferFactory.allocateBuffer(to - from);
		buffer.write(bytes, from, to - from);
		return buffer;
	}

}
