/*
 * Copyright 2002-2017 the original author or authors.
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
package org.springframework.web.servlet.mvc.method.annotation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;

import org.springframework.http.MediaType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event;


/**
 * Unit tests for {@link org.springframework.web.servlet.mvc.method.annotation.SseEmitter}.
 * @author Rossen Stoyanchev
 */
public class SseEmitterTests {

	private SseEmitter emitter;

	private TestHandler handler;


	@Before
	public void setup() throws IOException {
		this.handler = new TestHandler();
		this.emitter = new SseEmitter();
		this.emitter.initialize(this.handler);
	}


	@Test
	public void send() throws Exception {
		this.emitter.send("foo");
		this.handler.assertSentObjectCount(3);
		this.handler.assertObject(0, "data:", SseEmitter.TEXT_PLAIN);
		this.handler.assertObject(1, "foo");
		this.handler.assertObject(2, "\n\n", SseEmitter.TEXT_PLAIN);
	}

	@Test
	public void sendWithMediaType() throws Exception {
		this.emitter.send("foo", MediaType.TEXT_PLAIN);
		this.handler.assertSentObjectCount(3);
		this.handler.assertObject(0, "data:", SseEmitter.TEXT_PLAIN);
		this.handler.assertObject(1, "foo", MediaType.TEXT_PLAIN);
		this.handler.assertObject(2, "\n\n", SseEmitter.TEXT_PLAIN);
	}

	@Test
	public void sendEventEmpty() throws Exception {
		this.emitter.send(event());
		this.handler.assertSentObjectCount(0);
	}

	@Test
	public void sendEventWithDataLine() throws Exception {
		this.emitter.send(event().data("foo"));
		this.handler.assertSentObjectCount(3);
		this.handler.assertObject(0, "data:", SseEmitter.TEXT_PLAIN);
		this.handler.assertObject(1, "foo");
		this.handler.assertObject(2, "\n\n", SseEmitter.TEXT_PLAIN);
	}

	@Test
	public void sendEventWithTwoDataLines() throws Exception {
		this.emitter.send(event().data("foo").data("bar"));
		this.handler.assertSentObjectCount(5);
		this.handler.assertObject(0, "data:", SseEmitter.TEXT_PLAIN);
		this.handler.assertObject(1, "foo");
		this.handler.assertObject(2, "\ndata:", SseEmitter.TEXT_PLAIN);
		this.handler.assertObject(3, "bar");
		this.handler.assertObject(4, "\n\n", SseEmitter.TEXT_PLAIN);
	}

	@Test
	public void sendEventFull() throws Exception {
		this.emitter.send(event().comment("blah").name("test").reconnectTime(5000L).id("1").data("foo"));
		this.handler.assertSentObjectCount(3);
		this.handler.assertObject(0, ":blah\nevent:test\nretry:5000\nid:1\ndata:", SseEmitter.TEXT_PLAIN);
		this.handler.assertObject(1, "foo");
		this.handler.assertObject(2, "\n\n", SseEmitter.TEXT_PLAIN);
	}

	@Test
	public void sendEventFullWithTwoDataLinesInTheMiddle() throws Exception {
		this.emitter.send(event().comment("blah").data("foo").data("bar").name("test").reconnectTime(5000L).id("1"));
		this.handler.assertSentObjectCount(5);
		this.handler.assertObject(0, ":blah\ndata:", SseEmitter.TEXT_PLAIN);
		this.handler.assertObject(1, "foo");
		this.handler.assertObject(2, "\ndata:", SseEmitter.TEXT_PLAIN);
		this.handler.assertObject(3, "bar");
		this.handler.assertObject(4, "\nevent:test\nretry:5000\nid:1\n\n", SseEmitter.TEXT_PLAIN);
	}


	private static class TestHandler implements ResponseBodyEmitter.Handler {

		private List<Object> objects = new ArrayList<>();

		private List<MediaType> mediaTypes = new ArrayList<>();


		public void assertSentObjectCount(int size) {
			assertEquals(size, this.objects.size());
		}

		public void assertObject(int index, Object object) {
			assertObject(index, object, null);
		}

		public void assertObject(int index, Object object, MediaType mediaType) {
			assertTrue(index <= this.objects.size());
			assertEquals(object, this.objects.get(index));
			assertEquals(mediaType, this.mediaTypes.get(index));
		}

		@Override
		public void send(Object data, MediaType mediaType) throws IOException {
			this.objects.add(data);
			this.mediaTypes.add(mediaType);
		}

		@Override
		public void complete() {
		}

		@Override
		public void completeWithError(Throwable failure) {
		}

		@Override
		public void onTimeout(Runnable callback) {
		}

		@Override
		public void onError(Consumer<Throwable> callback) {
		}

		@Override
		public void onCompletion(Runnable callback) {
		}
	}

}
