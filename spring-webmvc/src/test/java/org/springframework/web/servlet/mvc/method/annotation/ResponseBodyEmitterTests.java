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

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.http.MediaType;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ResponseBodyEmitter}.
 *
 * @author Rossen Stoyanchev
 * @author Tomasz Nurkiewicz
 */
public class ResponseBodyEmitterTests {

	private ResponseBodyEmitter emitter;

	@Mock
	private ResponseBodyEmitter.Handler handler;


	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		this.emitter = new ResponseBodyEmitter();
	}


	@Test
	public void sendBeforeHandlerInitialized() throws Exception {
		this.emitter.send("foo", MediaType.TEXT_PLAIN);
		this.emitter.send("bar", MediaType.TEXT_PLAIN);
		this.emitter.complete();
		verifyNoMoreInteractions(this.handler);

		this.emitter.initialize(this.handler);
		verify(this.handler).send("foo", MediaType.TEXT_PLAIN);
		verify(this.handler).send("bar", MediaType.TEXT_PLAIN);
		verify(this.handler).complete();
		verifyNoMoreInteractions(this.handler);
	}

	@Test
	public void sendDuplicateBeforeHandlerInitialized() throws Exception {
		this.emitter.send("foo", MediaType.TEXT_PLAIN);
		this.emitter.send("foo", MediaType.TEXT_PLAIN);
		this.emitter.complete();
		verifyNoMoreInteractions(this.handler);

		this.emitter.initialize(this.handler);
		verify(this.handler, times(2)).send("foo", MediaType.TEXT_PLAIN);
		verify(this.handler).complete();
		verifyNoMoreInteractions(this.handler);
	}

	@Test
	public void sendBeforeHandlerInitializedWithError() throws Exception {
		IllegalStateException ex = new IllegalStateException();
		this.emitter.send("foo", MediaType.TEXT_PLAIN);
		this.emitter.send("bar", MediaType.TEXT_PLAIN);
		this.emitter.completeWithError(ex);
		verifyNoMoreInteractions(this.handler);

		this.emitter.initialize(this.handler);
		verify(this.handler).send("foo", MediaType.TEXT_PLAIN);
		verify(this.handler).send("bar", MediaType.TEXT_PLAIN);
		verify(this.handler).completeWithError(ex);
		verifyNoMoreInteractions(this.handler);
	}

	@Test(expected = IllegalStateException.class)
	public void sendFailsAfterComplete() throws Exception {
		this.emitter.complete();
		this.emitter.send("foo");
	}

	@Test
	public void sendAfterHandlerInitialized() throws Exception {
		this.emitter.initialize(this.handler);
		verify(this.handler).onTimeout(any());
		verify(this.handler).onCompletion(any());
		verifyNoMoreInteractions(this.handler);

		this.emitter.send("foo", MediaType.TEXT_PLAIN);
		this.emitter.send("bar", MediaType.TEXT_PLAIN);
		this.emitter.complete();

		verify(this.handler).send("foo", MediaType.TEXT_PLAIN);
		verify(this.handler).send("bar", MediaType.TEXT_PLAIN);
		verify(this.handler).complete();
		verifyNoMoreInteractions(this.handler);
	}

	@Test
	public void sendAfterHandlerInitializedWithError() throws Exception {
		this.emitter.initialize(this.handler);
		verify(this.handler).onTimeout(any());
		verify(this.handler).onCompletion(any());
		verifyNoMoreInteractions(this.handler);

		IllegalStateException ex = new IllegalStateException();
		this.emitter.send("foo", MediaType.TEXT_PLAIN);
		this.emitter.send("bar", MediaType.TEXT_PLAIN);
		this.emitter.completeWithError(ex);

		verify(this.handler).send("foo", MediaType.TEXT_PLAIN);
		verify(this.handler).send("bar", MediaType.TEXT_PLAIN);
		verify(this.handler).completeWithError(ex);
		verifyNoMoreInteractions(this.handler);
	}

	@Test
	public void sendWithError() throws Exception {
		this.emitter.initialize(this.handler);
		verify(this.handler).onTimeout(any());
		verify(this.handler).onCompletion(any());
		verifyNoMoreInteractions(this.handler);

		IOException failure = new IOException();
		doThrow(failure).when(this.handler).send("foo", MediaType.TEXT_PLAIN);
		try {
			this.emitter.send("foo", MediaType.TEXT_PLAIN);
			fail("Expected exception");
		}
		catch (IOException ex) {
			// expected
		}
		verify(this.handler).send("foo", MediaType.TEXT_PLAIN);
		verifyNoMoreInteractions(this.handler);
	}

	@Test
	public void onTimeoutBeforeHandlerInitialized() throws Exception  {
		Runnable runnable = mock(Runnable.class);
		this.emitter.onTimeout(runnable);
		this.emitter.initialize(this.handler);

		ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
		verify(this.handler).onTimeout(captor.capture());
		verify(this.handler).onCompletion(any());

		assertNotNull(captor.getValue());
		captor.getValue().run();
		verify(runnable).run();
	}

	@Test
	public void onTimeoutAfterHandlerInitialized() throws Exception  {
		this.emitter.initialize(this.handler);

		ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
		verify(this.handler).onTimeout(captor.capture());
		verify(this.handler).onCompletion(any());

		Runnable runnable = mock(Runnable.class);
		this.emitter.onTimeout(runnable);

		assertNotNull(captor.getValue());
		captor.getValue().run();
		verify(runnable).run();
	}

	@Test
	public void onCompletionBeforeHandlerInitialized() throws Exception  {
		Runnable runnable = mock(Runnable.class);
		this.emitter.onCompletion(runnable);
		this.emitter.initialize(this.handler);

		ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
		verify(this.handler).onTimeout(any());
		verify(this.handler).onCompletion(captor.capture());

		assertNotNull(captor.getValue());
		captor.getValue().run();
		verify(runnable).run();
	}

	@Test
	public void onCompletionAfterHandlerInitialized() throws Exception  {
		this.emitter.initialize(this.handler);

		ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
		verify(this.handler).onTimeout(any());
		verify(this.handler).onCompletion(captor.capture());

		Runnable runnable = mock(Runnable.class);
		this.emitter.onCompletion(runnable);

		assertNotNull(captor.getValue());
		captor.getValue().run();
		verify(runnable).run();
	}

}
