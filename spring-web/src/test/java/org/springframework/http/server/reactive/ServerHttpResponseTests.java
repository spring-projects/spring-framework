/*
 * Copyright 2002-2025 the original author or authors.
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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.channel.AbortedException;
import reactor.test.StepVerifier;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.core.testfixture.io.buffer.LeakAwareDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.codec.EncoderHttpMessageWriter;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.json.JacksonJsonEncoder;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AbstractServerHttpRequest}.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @author Brian Clozel
 */
class ServerHttpResponseTests {

	@Test
	void writeWith() {
		TestServerHttpResponse response = new TestServerHttpResponse();
		response.writeWith(Flux.just(wrap("a"), wrap("b"), wrap("c"))).block();

		assertThat(response.statusCodeWritten).isTrue();
		assertThat(response.headersWritten).isTrue();
		assertThat(response.cookiesWritten).isTrue();

		assertThat(response.body).hasSize(3);
		assertThat(response.body.get(0).toString(StandardCharsets.UTF_8)).isEqualTo("a");
		assertThat(response.body.get(1).toString(StandardCharsets.UTF_8)).isEqualTo("b");
		assertThat(response.body.get(2).toString(StandardCharsets.UTF_8)).isEqualTo("c");
	}

	@Test  // SPR-14952
	void writeAndFlushWithFluxOfDefaultDataBuffer() {
		TestServerHttpResponse response = new TestServerHttpResponse();
		Flux<Flux<DefaultDataBuffer>> flux = Flux.just(Flux.just(wrap("foo")));
		response.writeAndFlushWith(flux).block();

		assertThat(response.statusCodeWritten).isTrue();
		assertThat(response.headersWritten).isTrue();
		assertThat(response.cookiesWritten).isTrue();

		assertThat(response.body).hasSize(1);
		assertThat(response.body.get(0).toString(StandardCharsets.UTF_8)).isEqualTo("foo");
	}

	@Test
	void writeWithFluxError() {
		IllegalStateException error = new IllegalStateException("boo");
		writeWithError(Flux.error(error));
	}

	@Test
	void writeWithMonoError() {
		IllegalStateException error = new IllegalStateException("boo");
		writeWithError(Mono.error(error));
	}

	void writeWithError(Publisher<DataBuffer> body) {
		TestServerHttpResponse response = new TestServerHttpResponse();
		HttpHeaders headers = response.getHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set(HttpHeaders.CONTENT_ENCODING, "gzip");
		headers.setContentLength(12);
		response.writeWith(body).onErrorComplete().block();

		assertThat(response.statusCodeWritten).isFalse();
		assertThat(response.headersWritten).isFalse();
		assertThat(response.cookiesWritten).isFalse();
		assertThat(headers.headerNames()).doesNotContain(HttpHeaders.CONTENT_TYPE, HttpHeaders.CONTENT_LENGTH,
				HttpHeaders.CONTENT_ENCODING);
		assertThat(response.body).isEmpty();
	}

	@Test
	void setComplete() {
		TestServerHttpResponse response = new TestServerHttpResponse();
		response.setComplete().block();

		assertThat(response.statusCodeWritten).isTrue();
		assertThat(response.headersWritten).isTrue();
		assertThat(response.cookiesWritten).isTrue();
		assertThat(response.body).isEmpty();
	}

	@Test
	void beforeCommitWithComplete() {
		ResponseCookie cookie = ResponseCookie.from("ID", "123").build();
		TestServerHttpResponse response = new TestServerHttpResponse();
		response.beforeCommit(() -> Mono.fromRunnable(() -> response.getCookies().add(cookie.getName(), cookie)));
		response.writeWith(Flux.just(wrap("a"), wrap("b"), wrap("c"))).block();

		assertThat(response.statusCodeWritten).isTrue();
		assertThat(response.headersWritten).isTrue();
		assertThat(response.cookiesWritten).isTrue();
		assertThat(response.getCookies().getFirst("ID")).isSameAs(cookie);

		assertThat(response.body).hasSize(3);
		assertThat(response.body.get(0).toString(StandardCharsets.UTF_8)).isEqualTo("a");
		assertThat(response.body.get(1).toString(StandardCharsets.UTF_8)).isEqualTo("b");
		assertThat(response.body.get(2).toString(StandardCharsets.UTF_8)).isEqualTo("c");
	}

	@Test
	void beforeCommitActionWithSetComplete() {
		ResponseCookie cookie = ResponseCookie.from("ID", "123").build();
		TestServerHttpResponse response = new TestServerHttpResponse();
		response.beforeCommit(() -> {
			response.getCookies().add(cookie.getName(), cookie);
			return Mono.empty();
		});
		response.setComplete().block();

		assertThat(response.statusCodeWritten).isTrue();
		assertThat(response.headersWritten).isTrue();
		assertThat(response.cookiesWritten).isTrue();
		assertThat(response.body).isEmpty();
		assertThat(response.getCookies().getFirst("ID")).isSameAs(cookie);
	}

	@Test // gh-24186, gh-25753
	void beforeCommitErrorShouldLeaveResponseNotCommitted() {

		Consumer<Supplier<Mono<Void>>> tester = preCommitAction -> {
			TestServerHttpResponse response = new TestServerHttpResponse();
			response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
			response.getHeaders().setContentLength(3);
			response.beforeCommit(preCommitAction);

			StepVerifier.create(response.writeWith(Flux.just(wrap("body"))))
					.expectErrorMessage("Max sessions")
					.verify();

			assertThat(response.statusCodeWritten).isFalse();
			assertThat(response.headersWritten).isFalse();
			assertThat(response.cookiesWritten).isFalse();
			assertThat(response.isCommitted()).isFalse();
			assertThat(response.getHeaders().isEmpty()).isTrue();

			// Handle the error
			response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
			StepVerifier.create(response.setComplete()).verifyComplete();

			assertThat(response.statusCodeWritten).isTrue();
			assertThat(response.headersWritten).isTrue();
			assertThat(response.cookiesWritten).isTrue();
			assertThat(response.isCommitted()).isTrue();
		};

		tester.accept(() -> Mono.error(new IllegalStateException("Max sessions")));
		tester.accept(() -> {
			throw new IllegalStateException("Max sessions");
		});
	}

	@Test // gh-26232
	void monoResponseShouldNotLeakIfCancelled() {
		LeakAwareDataBufferFactory bufferFactory = new LeakAwareDataBufferFactory();
		MockServerHttpRequest request = MockServerHttpRequest.get("/").build();
		MockServerHttpResponse response = new MockServerHttpResponse(bufferFactory);
		response.setWriteHandler(flux -> {
			throw AbortedException.beforeSend();
		});

		HttpMessageWriter<Object> messageWriter = new EncoderHttpMessageWriter<>(new JacksonJsonEncoder());
		Mono<Void> result = messageWriter.write(Mono.just(Collections.singletonMap("foo", "bar")),
				ResolvableType.forClass(Mono.class), ResolvableType.forClass(Map.class), null,
				request, response, Collections.emptyMap());

		StepVerifier.create(result).expectError(AbortedException.class).verify();

		bufferFactory.checkForLeaks();
	}


	private DefaultDataBuffer wrap(String a) {
		return DefaultDataBufferFactory.sharedInstance.wrap(ByteBuffer.wrap(a.getBytes(StandardCharsets.UTF_8)));
	}


	private static class TestServerHttpResponse extends AbstractServerHttpResponse {

		private boolean statusCodeWritten;

		private boolean headersWritten;

		private boolean cookiesWritten;

		private final List<DataBuffer> body = new ArrayList<>();

		public TestServerHttpResponse() {
			super(DefaultDataBufferFactory.sharedInstance);
		}

		@Override
		public <T> T getNativeResponse() {
			throw new IllegalStateException("This is a mock. No running server, no native response.");
		}

		@Override
		public void applyStatusCode() {
			assertThat(this.statusCodeWritten).isFalse();
			this.statusCodeWritten = true;
		}

		@Override
		protected void applyHeaders() {
			assertThat(this.headersWritten).isFalse();
			this.headersWritten = true;
		}

		@Override
		protected void applyCookies() {
			assertThat(this.cookiesWritten).isFalse();
			this.cookiesWritten = true;
		}

		@Override
		protected Mono<Void> writeWithInternal(Publisher<? extends DataBuffer> body) {
			return Flux.from(body).map(b -> {
				this.body.add(b);
				return b;
			}).then();
		}

		@Override
		protected Mono<Void> writeAndFlushWithInternal(
				Publisher<? extends Publisher<? extends DataBuffer>> bodyWithFlush) {
			return Flux.from(bodyWithFlush).flatMap(body ->
				Flux.from(body).map(b -> {
					this.body.add(b);
					return b;
				})
			).then();
		}
	}

}
