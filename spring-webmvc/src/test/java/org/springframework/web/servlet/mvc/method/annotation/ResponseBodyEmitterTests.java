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

package org.springframework.web.servlet.mvc.method.annotation;

import java.io.IOException;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Tests for {@link ResponseBodyEmitter}.
 *
 * @author Rossen Stoyanchev
 * @author Tomasz Nurkiewicz
 * @author Brian Clozel
 */
@ExtendWith(MockitoExtension.class)
public class ResponseBodyEmitterTests {

	@Mock
	private ResponseBodyEmitter.Handler handler;

	private final ResponseBodyEmitter emitter = new ResponseBodyEmitter();


	@Test
	void sendBeforeHandlerInitialized() throws Exception {
		this.emitter.send("foo", MediaType.TEXT_PLAIN);
		this.emitter.send("bar", MediaType.TEXT_PLAIN);
		this.emitter.complete();
		verifyNoMoreInteractions(this.handler);

		this.emitter.initialize(this.handler);
		verify(this.handler).send(anySet());
		verify(this.handler).complete();
		verifyNoMoreInteractions(this.handler);
	}

	@Test
	void sendDuplicateBeforeHandlerInitialized() throws Exception {
		this.emitter.send("foo", MediaType.TEXT_PLAIN);
		this.emitter.send("foo", MediaType.TEXT_PLAIN);
		this.emitter.complete();
		verifyNoMoreInteractions(this.handler);

		this.emitter.initialize(this.handler);
		verify(this.handler).send(anySet());
		verify(this.handler).complete();
		verifyNoMoreInteractions(this.handler);
	}

	@Test
	void sendBeforeHandlerInitializedWithError() throws Exception {
		IllegalStateException ex = new IllegalStateException();
		this.emitter.send("foo", MediaType.TEXT_PLAIN);
		this.emitter.send("bar", MediaType.TEXT_PLAIN);
		this.emitter.completeWithError(ex);
		verifyNoMoreInteractions(this.handler);

		this.emitter.initialize(this.handler);
		verify(this.handler).send(anySet());
		verify(this.handler).completeWithError(ex);
		verifyNoMoreInteractions(this.handler);
	}

	@Test
	void sendFailsAfterComplete() {
		this.emitter.complete();
		assertThatIllegalStateException().isThrownBy(() -> this.emitter.send("foo"));
	}

	@Test
	void sendAfterHandlerInitialized() throws Exception {
		this.emitter.initialize(this.handler);
		verify(this.handler).onTimeout(any());
		verify(this.handler).onError(any());
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
	void sendAfterHandlerInitializedWithError() throws Exception {
		this.emitter.initialize(this.handler);
		verify(this.handler).onTimeout(any());
		verify(this.handler).onError(any());
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
	void sendWithError() throws Exception {
		this.emitter.initialize(this.handler);
		verify(this.handler).onTimeout(any());
		verify(this.handler).onError(any());
		verify(this.handler).onCompletion(any());
		verifyNoMoreInteractions(this.handler);

		willThrow(new IOException()).given(this.handler).send("foo", MediaType.TEXT_PLAIN);
		assertThatIOException().isThrownBy(() -> this.emitter.send("foo", MediaType.TEXT_PLAIN));
		verify(this.handler).send("foo", MediaType.TEXT_PLAIN);
		verifyNoMoreInteractions(this.handler);
	}

	@Test // gh-30687
	void completeAfterNonIOException() throws Exception {
		this.emitter.initialize(this.handler);
		verify(this.handler).onTimeout(any());
		verify(this.handler).onError(any());
		verify(this.handler).onCompletion(any());
		verifyNoMoreInteractions(this.handler);

		willThrow(new IllegalStateException()).given(this.handler).send("foo", MediaType.TEXT_PLAIN);
		assertThatIllegalStateException().isThrownBy(() -> this.emitter.send("foo", MediaType.TEXT_PLAIN));
		verify(this.handler).send("foo", MediaType.TEXT_PLAIN);
		verifyNoMoreInteractions(this.handler);

		this.emitter.complete();
		verify(this.handler).complete();
		verifyNoMoreInteractions(this.handler);
	}

	@Test
	void onTimeoutBeforeHandlerInitialized() throws Exception {
		Runnable runnable = mock();
		this.emitter.onTimeout(runnable);
		this.emitter.initialize(this.handler);

		ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
		verify(this.handler).onTimeout(captor.capture());
		verify(this.handler).onCompletion(any());

		assertThat(captor.getValue()).isNotNull();
		captor.getValue().run();
		verify(runnable).run();
	}

	@Test
	void onTimeoutAfterHandlerInitialized() throws Exception {
		this.emitter.initialize(this.handler);

		ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
		verify(this.handler).onTimeout(captor.capture());
		verify(this.handler).onCompletion(any());

		Runnable runnable = mock();
		this.emitter.onTimeout(runnable);

		assertThat(captor.getValue()).isNotNull();
		captor.getValue().run();
		verify(runnable).run();
	}

	@Test
	void multipleOnTimeoutCallbacks() throws Exception {
		this.emitter.initialize(this.handler);

		ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
		verify(this.handler).onTimeout(captor.capture());
		verify(this.handler).onCompletion(any());

		Runnable first = mock();
		Runnable second = mock();
		this.emitter.onTimeout(first);
		this.emitter.onTimeout(second);

		assertThat(captor.getValue()).isNotNull();
		captor.getValue().run();
		verify(first).run();
		verify(second).run();
	}

	@Test
	void onCompletionBeforeHandlerInitialized() throws Exception {
		Runnable runnable = mock();
		this.emitter.onCompletion(runnable);
		this.emitter.initialize(this.handler);

		ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
		verify(this.handler).onTimeout(any());
		verify(this.handler).onCompletion(captor.capture());

		assertThat(captor.getValue()).isNotNull();
		captor.getValue().run();
		verify(runnable).run();
	}

	@Test
	void onCompletionAfterHandlerInitialized() throws Exception {
		this.emitter.initialize(this.handler);

		ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
		verify(this.handler).onTimeout(any());
		verify(this.handler).onCompletion(captor.capture());

		Runnable runnable = mock();
		this.emitter.onCompletion(runnable);

		assertThat(captor.getValue()).isNotNull();
		captor.getValue().run();
		verify(runnable).run();
	}

	@Test
	void multipleOnCompletionCallbacks() throws Exception {
		this.emitter.initialize(this.handler);

		ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
		verify(this.handler).onTimeout(any());
		verify(this.handler).onCompletion(captor.capture());

		Runnable first = mock();
		Runnable second = mock();
		this.emitter.onCompletion(first);
		this.emitter.onCompletion(second);

		assertThat(captor.getValue()).isNotNull();
		captor.getValue().run();
		verify(first).run();
		verify(second).run();
	}

	@Test
	void multipleOnErrorCallbacks() throws Exception {
		this.emitter.initialize(this.handler);

		@SuppressWarnings({ "rawtypes", "unchecked" })
		ArgumentCaptor<Consumer<Throwable>> captor = ArgumentCaptor.<Consumer<Throwable>, Consumer>forClass(Consumer.class);
		verify(this.handler).onError(captor.capture());

		Consumer<Throwable> first = mock();
		Consumer<Throwable> second = mock();
		this.emitter.onError(first);
		this.emitter.onError(second);

		assertThat(captor.getValue()).isNotNull();
		IllegalStateException illegalStateException = new IllegalStateException();
		captor.getValue().accept(illegalStateException);
		verify(first).accept(eq(illegalStateException));
		verify(second).accept(eq(illegalStateException));
	}

}
