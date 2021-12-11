/*
 * Copyright 2002-2020 the original author or authors.
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event;


/**
 * Unit tests for {@link org.springframework.web.servlet.mvc.method.annotation.SseEmitter}.
 * @author Rossen Stoyanchev
 */
public class SseEmitterTests {

	private static final MediaType TEXT_PLAIN_UTF8 = new MediaType("text", "plain", StandardCharsets.UTF_8);


	private SseEmitter emitter;

	private TestHandler handler;


	@BeforeEach
	public void setup() throws IOException {
		this.handler = new TestHandler();
		this.emitter = new SseEmitter();
		this.emitter.initialize(this.handler);
	}


	@Test
	public void send() throws Exception {
		this.emitter.send("foo");
		this.handler.assertSentObjectCount(3);
		this.handler.assertObject(0, "data:", TEXT_PLAIN_UTF8);
		this.handler.assertObject(1, "foo");
		this.handler.assertObject(2, "\n\n", TEXT_PLAIN_UTF8);
	}

	@Test
	public void sendWithMediaType() throws Exception {
		this.emitter.send("foo", MediaType.TEXT_PLAIN);
		this.handler.assertSentObjectCount(3);
		this.handler.assertObject(0, "data:", TEXT_PLAIN_UTF8);
		this.handler.assertObject(1, "foo", MediaType.TEXT_PLAIN);
		this.handler.assertObject(2, "\n\n", TEXT_PLAIN_UTF8);
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
		this.handler.assertObject(0, "data:", TEXT_PLAIN_UTF8);
		this.handler.assertObject(1, "foo");
		this.handler.assertObject(2, "\n\n", TEXT_PLAIN_UTF8);
	}

	@Test
	public void sendEventWithTwoDataLines() throws Exception {
		this.emitter.send(event().data("foo").data("bar"));
		this.handler.assertSentObjectCount(5);
		this.handler.assertObject(0, "data:", TEXT_PLAIN_UTF8);
		this.handler.assertObject(1, "foo");
		this.handler.assertObject(2, "\ndata:", TEXT_PLAIN_UTF8);
		this.handler.assertObject(3, "bar");
		this.handler.assertObject(4, "\n\n", TEXT_PLAIN_UTF8);
	}

	@Test
	public void sendEventFull() throws Exception {
		this.emitter.send(event().comment("blah").name("test").reconnectTime(5000L).id("1").data("foo"));
		this.handler.assertSentObjectCount(3);
		this.handler.assertObject(0, ":blah\nevent:test\nretry:5000\nid:1\ndata:", TEXT_PLAIN_UTF8);
		this.handler.assertObject(1, "foo");
		this.handler.assertObject(2, "\n\n", TEXT_PLAIN_UTF8);
	}

	@Test
	public void sendEventFullWithTwoDataLinesInTheMiddle() throws Exception {
		this.emitter.send(event().comment("blah").data("foo").data("bar").name("test").reconnectTime(5000L).id("1"));
		this.handler.assertSentObjectCount(5);
		this.handler.assertObject(0, ":blah\ndata:", TEXT_PLAIN_UTF8);
		this.handler.assertObject(1, "foo");
		this.handler.assertObject(2, "\ndata:", TEXT_PLAIN_UTF8);
		this.handler.assertObject(3, "bar");
		this.handler.assertObject(4, "\nevent:test\nretry:5000\nid:1\n\n", TEXT_PLAIN_UTF8);
	}


	private static class TestHandler implements ResponseBodyEmitter.Handler {

		private List<Object> objects = new ArrayList<>();

		private List<MediaType> mediaTypes = new ArrayList<>();


		public void assertSentObjectCount(int size) {
			assertThat(this.objects.size()).isEqualTo(size);
		}

		public void assertObject(int index, Object object) {
			assertObject(index, object, null);
		}

		public void assertObject(int index, Object object, MediaType mediaType) {
			assertThat(index <= this.objects.size()).isTrue();
			assertThat(this.objects.get(index)).isEqualTo(object);
			assertThat(this.mediaTypes.get(index)).isEqualTo(mediaType);
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
