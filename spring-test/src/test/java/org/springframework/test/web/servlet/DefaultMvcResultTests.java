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

package org.springframework.test.web.servlet;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Test fixture for {@link DefaultMvcResult}.
 *
 * @author Rossen Stoyanchev
 */
class DefaultMvcResultTests {

	private final DefaultMvcResult mvcResult = new DefaultMvcResult(new MockHttpServletRequest(), null);

	@Test
	void getAsyncResultSuccess() {
		this.mvcResult.setAsyncResult("Foo");
		this.mvcResult.setAsyncDispatchLatch(new CountDownLatch(0));
		this.mvcResult.getAsyncResult();
	}

	@Test
	void getAsyncResultFailure() {
		assertThatIllegalStateException().isThrownBy(() ->
				this.mvcResult.getAsyncResult(0));
	}

	@Test
	void getAsyncResultRestoresInterruptStatusWhenInterrupted() {
		this.mvcResult.setAsyncDispatchLatch(new CountDownLatch(1));
		Thread.currentThread().interrupt();
		try {
			assertThatIllegalStateException().isThrownBy(() ->
					this.mvcResult.getAsyncResult(1000));
			assertThat(Thread.currentThread().isInterrupted()).isTrue();
		}
		finally {
			// Clear the interrupt status so it does not leak to other tests.
			Thread.interrupted();
		}
	}

	@ParameterizedTest  // gh-37453
	@Timeout(5)
	@ValueSource(longs = {-5, -1, 0})
	void getAsyncResultWithInfiniteAsyncRequestTimeout(long timeout) {
		DefaultMvcResult mvcResult = createMvcResultWithAsyncRequestTimeout(timeout);
		CountDownLatch latch = new CountDownLatch(1);
		mvcResult.setAsyncDispatchLatch(latch);
		CompletableFuture.runAsync(() -> {
			mvcResult.setAsyncResult("Foo");
			latch.countDown();
		}, CompletableFuture.delayedExecutor(100, TimeUnit.MILLISECONDS));

		assertThat(mvcResult.getAsyncResult()).isEqualTo("Foo");
	}

	@Test  // gh-37453
	@Timeout(5)
	void getAsyncResultWithExplicitTimeToWaitOfZeroDoesNotWait() {
		DefaultMvcResult mvcResult = createMvcResultWithAsyncRequestTimeout(0);
		mvcResult.setAsyncDispatchLatch(new CountDownLatch(1));

		assertThatIllegalStateException()
				.isThrownBy(() -> mvcResult.getAsyncResult(0))
				.withMessageEndingWith("was not set during the specified timeToWait=0");
	}

	@Test  // gh-37453
	void getAsyncResultWithAsyncRequestTimeoutExceeded() {
		DefaultMvcResult mvcResult = createMvcResultWithAsyncRequestTimeout(50);
		mvcResult.setAsyncDispatchLatch(new CountDownLatch(1));

		assertThatIllegalStateException()
				.isThrownBy(mvcResult::getAsyncResult)
				.withMessageEndingWith("was not set during the specified timeToWait=50");
	}


	private static DefaultMvcResult createMvcResultWithAsyncRequestTimeout(long timeout) {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAsyncSupported(true);
		request.startAsync().setTimeout(timeout);
		return new DefaultMvcResult(request, null);
	}

}
