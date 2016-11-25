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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.ResponseCookie;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

/**
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 */
public class ServerHttpResponseTests {

	@Test
	public void writeWith() throws Exception {
		TestServerHttpResponse response = new TestServerHttpResponse();
		response.writeWith(Flux.just(wrap("a"), wrap("b"), wrap("c"))).block();

		assertTrue(response.statusCodeWritten);
		assertTrue(response.headersWritten);
		assertTrue(response.cookiesWritten);

		assertEquals(3, response.body.size());
		assertEquals("a", new String(response.body.get(0).asByteBuffer().array(), StandardCharsets.UTF_8));
		assertEquals("b", new String(response.body.get(1).asByteBuffer().array(), StandardCharsets.UTF_8));
		assertEquals("c", new String(response.body.get(2).asByteBuffer().array(), StandardCharsets.UTF_8));
	}

	@Test  // SPR-14952
	public void writeAndFlushWithFluxOfDefaultDataBuffer() throws Exception {
		TestServerHttpResponse response = new TestServerHttpResponse();
		Flux<Flux<DefaultDataBuffer>> flux = Flux.just(Flux.just(wrap("foo")));
		response.writeAndFlushWith(flux).block();

		assertTrue(response.statusCodeWritten);
		assertTrue(response.headersWritten);
		assertTrue(response.cookiesWritten);

		assertEquals(1, response.body.size());
		assertEquals("foo", new String(response.body.get(0).asByteBuffer().array(), StandardCharsets.UTF_8));
	}

	@Test
	public void writeWithError() throws Exception {
		TestServerHttpResponse response = new TestServerHttpResponse();
		IllegalStateException error = new IllegalStateException("boo");
		response.writeWith(Flux.error(error)).otherwise(ex -> Mono.empty()).block();

		assertFalse(response.statusCodeWritten);
		assertFalse(response.headersWritten);
		assertFalse(response.cookiesWritten);
		assertTrue(response.body.isEmpty());
	}

	@Test
	public void setComplete() throws Exception {
		TestServerHttpResponse response = new TestServerHttpResponse();
		response.setComplete().block();

		assertTrue(response.statusCodeWritten);
		assertTrue(response.headersWritten);
		assertTrue(response.cookiesWritten);
		assertTrue(response.body.isEmpty());
	}

	@Test
	public void beforeCommitWithComplete() throws Exception {
		ResponseCookie cookie = ResponseCookie.from("ID", "123").build();
		TestServerHttpResponse response = new TestServerHttpResponse();
		response.beforeCommit(() -> {
			response.getCookies().add(cookie.getName(), cookie);
			return Mono.empty();
		});
		response.writeWith(Flux.just(wrap("a"), wrap("b"), wrap("c"))).block();

		assertTrue(response.statusCodeWritten);
		assertTrue(response.headersWritten);
		assertTrue(response.cookiesWritten);
		assertSame(cookie, response.getCookies().getFirst("ID"));

		assertEquals(3, response.body.size());
		assertEquals("a", new String(response.body.get(0).asByteBuffer().array(), StandardCharsets.UTF_8));
		assertEquals("b", new String(response.body.get(1).asByteBuffer().array(), StandardCharsets.UTF_8));
		assertEquals("c", new String(response.body.get(2).asByteBuffer().array(), StandardCharsets.UTF_8));
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

		assertTrue(response.statusCodeWritten);
		assertTrue(response.headersWritten);
		assertTrue(response.cookiesWritten);
		assertTrue(response.body.isEmpty());
		assertSame(cookie, response.getCookies().getFirst("ID"));
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
		public void applyStatusCode() {
			assertFalse(this.statusCodeWritten);
			this.statusCodeWritten = true;
		}

		@Override
		protected void applyHeaders() {
			assertFalse(this.headersWritten);
			this.headersWritten = true;
		}

		@Override
		protected void applyCookies() {
			assertFalse(this.cookiesWritten);
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
