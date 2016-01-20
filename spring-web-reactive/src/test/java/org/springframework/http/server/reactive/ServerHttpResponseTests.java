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
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferAllocator;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * @author Rossen Stoyanchev
 */
public class ServerHttpResponseTests {

	public static final Charset UTF_8 = Charset.forName("UTF-8");


	@Test
	public void setBody() throws Exception {
		TestServerHttpResponse response = new TestServerHttpResponse();
		response.setBody(Flux.just(wrap("a"), wrap("b"), wrap("c"))).get();

		assertTrue(response.headersWritten);
		assertTrue(response.cookiesWritten);

		assertEquals(3, response.content.size());
		assertEquals("a", new String(response.content.get(0).asByteBuffer().array(), UTF_8));
		assertEquals("b", new String(response.content.get(1).asByteBuffer().array(), UTF_8));
		assertEquals("c", new String(response.content.get(2).asByteBuffer().array(), UTF_8));
	}

	@Test
	public void setBodyWithError() throws Exception {
		TestServerHttpResponse response = new TestServerHttpResponse();
		IllegalStateException error = new IllegalStateException("boo");
		response.setBody(Flux.error(error)).otherwise(ex -> Mono.empty()).get();

		assertFalse(response.headersWritten);
		assertFalse(response.cookiesWritten);
		assertTrue(response.content.isEmpty());
	}

	@Test
	public void setComplete() throws Exception {
		TestServerHttpResponse response = new TestServerHttpResponse();
		response.setComplete().get();

		assertTrue(response.headersWritten);
		assertTrue(response.cookiesWritten);
		assertTrue(response.content.isEmpty());
	}

	@Test
	public void beforeCommitWithSetBody() throws Exception {
		HttpCookie cookie = HttpCookie.serverCookie("ID", "123").build();
		TestServerHttpResponse response = new TestServerHttpResponse();
		response.beforeCommit(() -> {
			response.getHeaders().addCookie(cookie);
			return Mono.empty();
		});
		response.setBody(Flux.just(wrap("a"), wrap("b"), wrap("c"))).get();

		assertTrue(response.headersWritten);
		assertTrue(response.cookiesWritten);
		assertSame(cookie, response.getHeaders().getCookies().get("ID").get(0));

		assertEquals(3, response.content.size());
		assertEquals("a", new String(response.content.get(0).asByteBuffer().array(), UTF_8));
		assertEquals("b", new String(response.content.get(1).asByteBuffer().array(), UTF_8));
		assertEquals("c", new String(response.content.get(2).asByteBuffer().array(), UTF_8));
	}

	@Test
	public void beforeCommitActionWithError() throws Exception {
		TestServerHttpResponse response = new TestServerHttpResponse();
		IllegalStateException error = new IllegalStateException("boo");
		response.beforeCommit(() -> Mono.error(error));
		response.setBody(Flux.just(wrap("a"), wrap("b"), wrap("c"))).get();

		assertTrue("beforeCommit action errors should be ignored", response.headersWritten);
		assertTrue("beforeCommit action errors should be ignored", response.cookiesWritten);
		assertNull(response.getHeaders().getCookies().get("ID"));

		assertEquals(3, response.content.size());
		assertEquals("a", new String(response.content.get(0).asByteBuffer().array(), UTF_8));
		assertEquals("b", new String(response.content.get(1).asByteBuffer().array(), UTF_8));
		assertEquals("c", new String(response.content.get(2).asByteBuffer().array(), UTF_8));
	}

	@Test
	public void beforeCommitActionWithSetComplete() throws Exception {
		HttpCookie cookie = HttpCookie.serverCookie("ID", "123").build();
		TestServerHttpResponse response = new TestServerHttpResponse();
		response.beforeCommit(() -> {
			response.getHeaders().addCookie(cookie);
			return Mono.empty();
		});
		response.setComplete().get();

		assertTrue(response.headersWritten);
		assertTrue(response.cookiesWritten);
		assertTrue(response.content.isEmpty());
		assertSame(cookie, response.getHeaders().getCookies().get("ID").get(0));
	}



	private DataBuffer wrap(String a) {
		return new DefaultDataBufferAllocator().wrap(ByteBuffer.wrap(a.getBytes(UTF_8)));
	}


	private static class TestServerHttpResponse extends AbstractServerHttpResponse {

		private boolean headersWritten;

		private boolean cookiesWritten;

		private final List<DataBuffer> content = new ArrayList<>();


		@Override
		public void setStatusCode(HttpStatus status) {
		}

		@Override
		protected void writeHeaders() {
			assertFalse(this.headersWritten);
			this.headersWritten = true;
		}

		@Override
		protected void writeCookies() {
			assertFalse(this.cookiesWritten);
			this.cookiesWritten = true;
		}

		@Override
		protected Mono<Void> setBodyInternal(Publisher<DataBuffer> publisher) {
			return Flux.from(publisher).map(b -> {
				this.content.add(b);
				return b;
			}).after();
		}
	}

}
