/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.web.context.request.async;

import org.junit.jupiter.api.Test;

import org.springframework.web.context.request.async.DeferredResult.DeferredResultHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * DeferredResult tests.
 *
 * @author Rossen Stoyanchev
 */
class DeferredResultTests {

	@Test
	void setResult() {
		DeferredResultHandler handler = mock();

		DeferredResult<String> result = new DeferredResult<>();
		result.setResultHandler(handler);

		assertThat(result.setResult("hello")).isTrue();
		verify(handler).handleResult("hello");
	}

	@Test
	void setResultTwice() {
		DeferredResultHandler handler = mock();

		DeferredResult<String> result = new DeferredResult<>();
		result.setResultHandler(handler);

		assertThat(result.setResult("hello")).isTrue();
		assertThat(result.setResult("hi")).isFalse();

		verify(handler).handleResult("hello");
	}

	@Test
	void isSetOrExpired() {
		DeferredResultHandler handler = mock();

		DeferredResult<String> result = new DeferredResult<>();
		result.setResultHandler(handler);

		assertThat(result.isSetOrExpired()).isFalse();

		result.setResult("hello");

		assertThat(result.isSetOrExpired()).isTrue();

		verify(handler).handleResult("hello");
	}

	@Test
	void hasResult() {
		DeferredResultHandler handler = mock();

		DeferredResult<String> result = new DeferredResult<>();
		result.setResultHandler(handler);

		assertThat(result.hasResult()).isFalse();
		assertThat(result.getResult()).isNull();

		result.setResult("hello");

		assertThat(result.getResult()).isEqualTo("hello");
	}

	@Test
	void onCompletion() throws Exception {
		final StringBuilder sb = new StringBuilder();

		DeferredResult<String> result = new DeferredResult<>();
		result.onCompletion(() -> sb.append("completion event"));

		result.getInterceptor().afterCompletion(null, null);

		assertThat(result.isSetOrExpired()).isTrue();
		assertThat(sb.toString()).isEqualTo("completion event");
	}

	@Test
	void onTimeout() throws Exception {
		final StringBuilder sb = new StringBuilder();

		DeferredResultHandler handler = mock();

		DeferredResult<String> result = new DeferredResult<>(null, "timeout result");
		result.setResultHandler(handler);
		result.onTimeout(() -> sb.append("timeout event"));

		result.getInterceptor().handleTimeout(null, null);

		assertThat(sb.toString()).isEqualTo("timeout event");
		assertThat(result.setResult("hello")).as("Should not be able to set result a second time").isFalse();
		verify(handler).handleResult("timeout result");
	}

	@Test
	void onError() throws Exception {
		final StringBuilder sb = new StringBuilder();

		DeferredResultHandler handler = mock();

		DeferredResult<String> result = new DeferredResult<>(null, "error result");
		result.setResultHandler(handler);
		Exception e = new Exception();
		result.onError(t -> sb.append("error event"));

		result.getInterceptor().handleError(null, null, e);

		assertThat(sb.toString()).isEqualTo("error event");
		assertThat(result.setResult("hello")).as("Should not be able to set result a second time").isFalse();
		verify(handler).handleResult(e);
	}

}
