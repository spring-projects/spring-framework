/*
 * Copyright 2002-2019 the original author or authors.
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
import java.util.List;

import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 */
public class ServerHttpResponseTests {

	@Test
	public void writeWith() throws Exception {
		TestServerHttpResponse response = new TestServerHttpResponse();
		response.writeWith(Flux.just(wrap("a"), wrap("b"), wrap("c"))).block();

		assertThat(response.statusCodeWritten).isTrue();
		assertThat(response.headersWritten).isTrue();
		assertThat(response.cookiesWritten).isTrue();

		assertThat(response.body.size()).isEqualTo(3);
		assertThat(new String(response.body.get(0).asByteBuffer().array(), StandardCharsets.UTF_8)).isEqualTo("a");
		assertThat(new String(response.body.get(1).asByteBuffer().array(), StandardCharsets.UTF_8)).isEqualTo("b");
		assertThat(new String(response.body.get(2).asByteBuffer().array(), StandardCharsets.UTF_8)).isEqualTo("c");
	}

	@Test  // SPR-14952
	public void writeAndFlushWithFluxOfDefaultDataBuffer() throws Exception {
		TestServerHttpResponse response = new TestServerHttpResponse();
		Flux<Flux<DefaultDataBuffer>> flux = Flux.just(Flux.just(wrap("foo")));
		response.writeAndFlushWith(flux).block();

		assertThat(response.statusCodeWritten).isTrue();
		assertThat(response.headersWritten).isTrue();
		assertThat(response.cookiesWritten).isTrue();

		assertThat(response.body.size()).isEqualTo(1);
		assertThat(new String(response.body.get(0).asByteBuffer().array(), StandardCharsets.UTF_8)).isEqualTo("foo");
	}

	@Test
	public void writeWithError() throws Exception {
		TestServerHttpResponse response = new TestServerHttpResponse();
		response.getHeaders().setContentLength(12);
		IllegalStateException error = new IllegalStateException("boo");
		response.writeWith(Flux.error(error)).onErrorResume(ex -> Mono.empty()).block();

		assertThat(response.statusCodeWritten).isFalse();
		assertThat(response.headersWritten).isFalse();
		assertThat(response.cookiesWritten).isFalse();
		assertThat(response.getHeaders().containsKey(HttpHeaders.CONTENT_LENGTH)).isFalse();
		assertThat(response.body.isEmpty()).isTrue();
	}

	@Test
	public void setComplete() throws Exception {
		TestServerHttpResponse response = new TestServerHttpResponse();
		response.setComplete().block();

		assertThat(response.statusCodeWritten).isTrue();
		assertThat(response.headersWritten).isTrue();
		assertThat(response.cookiesWritten).isTrue();
		assertThat(response.body.isEmpty()).isTrue();
	}

	@Test
	public void beforeCommitWithComplete() throws Exception {
		ResponseCookie cookie = ResponseCookie.from("ID", "123").build();
		TestServerHttpResponse response = new TestServerHttpResponse();
		response.beforeCommit(() -> Mono.fromRunnable(() -> response.getCookies().add(cookie.getName(), cookie)));
		response.writeWith(Flux.just(wrap("a"), wrap("b"), wrap("c"))).block();

		assertThat(response.statusCodeWritten).isTrue();
		assertThat(response.headersWritten).isTrue();
		assertThat(response.cookiesWritten).isTrue();
		assertThat(response.getCookies().getFirst("ID")).isSameAs(cookie);

		assertThat(response.body.size()).isEqualTo(3);
		assertThat(new String(response.body.get(0).asByteBuffer().array(), StandardCharsets.UTF_8)).isEqualTo("a");
		assertThat(new String(response.body.get(1).asByteBuffer().array(), StandardCharsets.UTF_8)).isEqualTo("b");
		assertThat(new String(response.body.get(2).asByteBuffer().array(), StandardCharsets.UTF_8)).isEqualTo("c");
	}

	@Test
	public void beforeCommitActionWithSetComplete() throws Exception {
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
		assertThat(response.body.isEmpty()).isTrue();
		assertThat(response.getCookies().getFirst("ID")).isSameAs(cookie);
	}



	private DefaultDataBuffer wrap(String a) {
		return new DefaultDataBufferFactory().wrap(ByteBuffer.wrap(a.getBytes(StandardCharsets.UTF_8)));
	}


	private static class TestServerHttpResponse extends AbstractServerHttpResponse {

		private boolean statusCodeWritten;

		private boolean headersWritten;

		private boolean cookiesWritten;

		private final List<DataBuffer> body = new ArrayList<>();

		public TestServerHttpResponse() {
			super(new DefaultDataBufferFactory());
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
