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

package org.springframework.web.context.request.async;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link WebAsyncManager} with a mock {@link AsyncWebRequest}.
 *
 * @author Rossen Stoyanchev
 */
class WebAsyncManagerTests {

	private final AsyncWebRequest asyncWebRequest = mock();

	private final MockHttpServletRequest servletRequest = new MockHttpServletRequest();

	private final WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(servletRequest);


	@BeforeEach
	void setup() {
		this.asyncManager.setTaskExecutor(new SyncTaskExecutor());
		this.asyncManager.setAsyncWebRequest(this.asyncWebRequest);
		verify(this.asyncWebRequest).addCompletionHandler(notNull());
		reset(this.asyncWebRequest);
	}


	@Test
	void startAsyncProcessingWithoutAsyncWebRequest() {
		WebAsyncManager manager = WebAsyncUtils.getAsyncManager(new MockHttpServletRequest());

		assertThatIllegalStateException()
			.isThrownBy(() -> manager.startCallableProcessing(new StubCallable(1)))
			.withMessage("AsyncWebRequest must not be null");

		assertThatIllegalStateException()
			.isThrownBy(() -> manager.startDeferredResultProcessing(new DeferredResult<>()))
			.withMessage("AsyncWebRequest must not be null");
	}

	@Test
	void isConcurrentHandlingStarted() {
		given(this.asyncWebRequest.isAsyncStarted()).willReturn(false);

		assertThat(this.asyncManager.isConcurrentHandlingStarted()).isFalse();

		reset(this.asyncWebRequest);
		given(this.asyncWebRequest.isAsyncStarted()).willReturn(true);

		assertThat(this.asyncManager.isConcurrentHandlingStarted()).isTrue();
	}

	@Test
	void setAsyncWebRequestAfterAsyncStarted() {
		this.asyncWebRequest.startAsync();
		assertThatIllegalArgumentException().isThrownBy(() -> this.asyncManager.setAsyncWebRequest(null));
	}

	@Test
	void startCallableProcessing() throws Exception {
		int concurrentResult = 21;
		Callable<Object> task = new StubCallable(concurrentResult);

		CallableProcessingInterceptor interceptor = mock();

		setupDefaultAsyncScenario();

		this.asyncManager.registerCallableInterceptor("interceptor", interceptor);
		this.asyncManager.startCallableProcessing(task);

		assertThat(this.asyncManager.hasConcurrentResult()).isTrue();
		assertThat(this.asyncManager.getConcurrentResult()).isEqualTo(concurrentResult);

		verifyDefaultAsyncScenario();
		verify(interceptor).beforeConcurrentHandling(this.asyncWebRequest, task);
		verify(interceptor).preProcess(this.asyncWebRequest, task);
		verify(interceptor).postProcess(this.asyncWebRequest, task, concurrentResult);
	}

	@Test
	void startCallableProcessingCallableException() throws Exception {
		Exception concurrentResult = new Exception();
		Callable<Object> task = new StubCallable(concurrentResult);

		CallableProcessingInterceptor interceptor = mock();

		setupDefaultAsyncScenario();

		this.asyncManager.registerCallableInterceptor("interceptor", interceptor);
		this.asyncManager.startCallableProcessing(task);

		assertThat(this.asyncManager.hasConcurrentResult()).isTrue();
		assertThat(this.asyncManager.getConcurrentResult()).isEqualTo(concurrentResult);

		verifyDefaultAsyncScenario();
		verify(interceptor).beforeConcurrentHandling(this.asyncWebRequest, task);
		verify(interceptor).preProcess(this.asyncWebRequest, task);
		verify(interceptor).postProcess(this.asyncWebRequest, task, concurrentResult);
	}

	@Test // gh-30232
	void startCallableProcessingSubmitException() throws Exception {
		RuntimeException ex = new RuntimeException();

		setupDefaultAsyncScenario();

		this.asyncManager.setTaskExecutor(new SimpleAsyncTaskExecutor() {
			@Override
			public Future<?> submit(Runnable task) {
				throw ex;
			}
		});
		this.asyncManager.startCallableProcessing(() -> "not used");

		assertThat(this.asyncManager.hasConcurrentResult()).isTrue();
		assertThat(this.asyncManager.getConcurrentResult()).isEqualTo(ex);

		verifyDefaultAsyncScenario();
	}

	@Test
	void startCallableProcessingBeforeConcurrentHandlingException() throws Exception {
		Callable<Object> task = new StubCallable(21);
		Exception exception = new Exception();

		CallableProcessingInterceptor interceptor = mock();
		willThrow(exception).given(interceptor).beforeConcurrentHandling(this.asyncWebRequest, task);

		this.asyncManager.registerCallableInterceptor("interceptor", interceptor);

		assertThatException()
			.isThrownBy(() -> this.asyncManager.startCallableProcessing(task))
			.isEqualTo(exception);

		assertThat(this.asyncManager.hasConcurrentResult()).isFalse();

		verify(this.asyncWebRequest).addTimeoutHandler(notNull());
		verify(this.asyncWebRequest).addErrorHandler(notNull());
		verify(this.asyncWebRequest).addCompletionHandler(notNull());
	}

	@Test
	void startCallableProcessingPreProcessException() throws Exception {
		Callable<Object> task = new StubCallable(21);
		Exception exception = new Exception();

		CallableProcessingInterceptor interceptor = mock();
		willThrow(exception).given(interceptor).preProcess(this.asyncWebRequest, task);

		setupDefaultAsyncScenario();

		this.asyncManager.registerCallableInterceptor("interceptor", interceptor);
		this.asyncManager.startCallableProcessing(task);

		assertThat(this.asyncManager.hasConcurrentResult()).isTrue();
		assertThat(this.asyncManager.getConcurrentResult()).isEqualTo(exception);

		verifyDefaultAsyncScenario();
		verify(interceptor).beforeConcurrentHandling(this.asyncWebRequest, task);
	}

	@Test
	void startCallableProcessingPostProcessException() throws Exception {
		Callable<Object> task = new StubCallable(21);
		Exception exception = new Exception();

		CallableProcessingInterceptor interceptor = mock();
		willThrow(exception).given(interceptor).postProcess(this.asyncWebRequest, task, 21);

		setupDefaultAsyncScenario();

		this.asyncManager.registerCallableInterceptor("interceptor", interceptor);
		this.asyncManager.startCallableProcessing(task);

		assertThat(this.asyncManager.hasConcurrentResult()).isTrue();
		assertThat(this.asyncManager.getConcurrentResult()).isEqualTo(exception);

		verifyDefaultAsyncScenario();
		verify(interceptor).beforeConcurrentHandling(this.asyncWebRequest, task);
		verify(interceptor).preProcess(this.asyncWebRequest, task);
	}

	@Test
	void startCallableProcessingPostProcessContinueAfterException() throws Exception {
		Callable<Object> task = new StubCallable(21);
		Exception exception = new Exception();

		CallableProcessingInterceptor interceptor1 = mock();
		CallableProcessingInterceptor interceptor2 = mock();
		willThrow(exception).given(interceptor2).postProcess(this.asyncWebRequest, task, 21);

		setupDefaultAsyncScenario();

		this.asyncManager.registerCallableInterceptors(interceptor1, interceptor2);
		this.asyncManager.startCallableProcessing(task);

		assertThat(this.asyncManager.hasConcurrentResult()).isTrue();
		assertThat(this.asyncManager.getConcurrentResult()).isEqualTo(exception);

		verifyDefaultAsyncScenario();
		verify(interceptor1).beforeConcurrentHandling(this.asyncWebRequest, task);
		verify(interceptor1).preProcess(this.asyncWebRequest, task);
		verify(interceptor1).postProcess(this.asyncWebRequest, task, 21);
		verify(interceptor2).beforeConcurrentHandling(this.asyncWebRequest, task);
		verify(interceptor2).preProcess(this.asyncWebRequest, task);
	}

	@SuppressWarnings("unchecked")
	@Test
	void startCallableProcessingWithAsyncTask() throws Exception {
		AsyncTaskExecutor executor = mock();
		given(this.asyncWebRequest.getNativeRequest(HttpServletRequest.class)).willReturn(this.servletRequest);

		WebAsyncTask<Object> asyncTask = new WebAsyncTask<>(1000L, executor, mock());
		this.asyncManager.startCallableProcessing(asyncTask);

		verify(executor).submit((Runnable) notNull());
		verify(this.asyncWebRequest).setTimeout(1000L);
		verify(this.asyncWebRequest).addTimeoutHandler(any(Runnable.class));
		verify(this.asyncWebRequest).addErrorHandler(any(Consumer.class));
		verify(this.asyncWebRequest).addCompletionHandler(any(Runnable.class));
		verify(this.asyncWebRequest).startAsync();
	}

	@Test
	void startCallableProcessingNullInput() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> this.asyncManager.startCallableProcessing((Callable<?>) null))
			.withMessage("Callable must not be null");
	}

	@Test
	void startDeferredResultProcessing() throws Exception {
		DeferredResult<String> deferredResult = new DeferredResult<>(1000L);
		String concurrentResult = "abc";

		DeferredResultProcessingInterceptor interceptor = mock();

		setupDefaultAsyncScenario();

		this.asyncManager.registerDeferredResultInterceptor("interceptor", interceptor);
		this.asyncManager.startDeferredResultProcessing(deferredResult);

		deferredResult.setResult(concurrentResult);

		assertThat(this.asyncManager.getConcurrentResult()).isEqualTo(concurrentResult);
		verifyDefaultAsyncScenario();
		verify(interceptor).beforeConcurrentHandling(this.asyncWebRequest, deferredResult);
		verify(interceptor).preProcess(this.asyncWebRequest, deferredResult);
		verify(interceptor).postProcess(asyncWebRequest, deferredResult, concurrentResult);
		verify(this.asyncWebRequest).setTimeout(1000L);
	}

	@Test
	void startDeferredResultProcessingBeforeConcurrentHandlingException() throws Exception {
		DeferredResult<Integer> deferredResult = new DeferredResult<>();
		Exception exception = new Exception();

		DeferredResultProcessingInterceptor interceptor = mock();
		willThrow(exception).given(interceptor).beforeConcurrentHandling(this.asyncWebRequest, deferredResult);

		this.asyncManager.registerDeferredResultInterceptor("interceptor", interceptor);

		assertThatException()
			.isThrownBy(() -> this.asyncManager.startDeferredResultProcessing(deferredResult))
			.isEqualTo(exception);

		assertThat(this.asyncManager.hasConcurrentResult()).isFalse();

		verify(this.asyncWebRequest).addTimeoutHandler(notNull());
		verify(this.asyncWebRequest).addErrorHandler(notNull());
		verify(this.asyncWebRequest).addCompletionHandler(notNull());
	}

	@Test
	void startDeferredResultProcessingPreProcessException() throws Exception {
		DeferredResult<Integer> deferredResult = new DeferredResult<>();
		Exception exception = new Exception();

		DeferredResultProcessingInterceptor interceptor = mock();
		willThrow(exception).given(interceptor).preProcess(this.asyncWebRequest, deferredResult);

		setupDefaultAsyncScenario();

		this.asyncManager.registerDeferredResultInterceptor("interceptor", interceptor);
		this.asyncManager.startDeferredResultProcessing(deferredResult);

		deferredResult.setResult(25);

		assertThat(this.asyncManager.getConcurrentResult()).isEqualTo(exception);
		verifyDefaultAsyncScenario();
		verify(interceptor).beforeConcurrentHandling(this.asyncWebRequest, deferredResult);
	}

	@Test
	void startDeferredResultProcessingPostProcessException() throws Exception {
		DeferredResult<Integer> deferredResult = new DeferredResult<>();
		Exception exception = new Exception();

		DeferredResultProcessingInterceptor interceptor = mock();
		willThrow(exception).given(interceptor).postProcess(this.asyncWebRequest, deferredResult, 25);

		setupDefaultAsyncScenario();

		this.asyncManager.registerDeferredResultInterceptor("interceptor", interceptor);
		this.asyncManager.startDeferredResultProcessing(deferredResult);

		deferredResult.setResult(25);

		assertThat(this.asyncManager.getConcurrentResult()).isEqualTo(exception);
		verifyDefaultAsyncScenario();
		verify(interceptor).beforeConcurrentHandling(this.asyncWebRequest, deferredResult);
		verify(interceptor).preProcess(this.asyncWebRequest, deferredResult);
	}

	@Test
	void startDeferredResultProcessingNullInput() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> this.asyncManager.startDeferredResultProcessing(null))
			.withMessage("DeferredResult must not be null");
	}

	private void setupDefaultAsyncScenario() {
		given(this.asyncWebRequest.getNativeRequest(HttpServletRequest.class)).willReturn(this.servletRequest);
		given(this.asyncWebRequest.isAsyncComplete()).willReturn(false);
	}

	private void verifyDefaultAsyncScenario() {
		verify(this.asyncWebRequest).addTimeoutHandler(notNull());
		verify(this.asyncWebRequest).addErrorHandler(notNull());
		verify(this.asyncWebRequest).addCompletionHandler(notNull());
		verify(this.asyncWebRequest).startAsync();
		verify(this.asyncWebRequest).dispatch();
	}


	private static final class StubCallable implements Callable<Object> {

		private final Object value;

		StubCallable(Object value) {
			this.value = value;
		}

		@Override
		public Object call() throws Exception {
			if (this.value instanceof Exception) {
				throw ((Exception) this.value);
			}
			return this.value;
		}
	}


	@SuppressWarnings("serial")
	private static class SyncTaskExecutor extends SimpleAsyncTaskExecutor {

		@Override
		@SuppressWarnings("deprecation")
		public void execute(Runnable task, long startTimeout) {
			task.run();
		}
	}

}
