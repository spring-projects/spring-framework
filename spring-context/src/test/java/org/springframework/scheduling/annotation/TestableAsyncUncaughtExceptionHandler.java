/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scheduling.annotation;

import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * An {@link AsyncUncaughtExceptionHandler} implementation used for testing purposes.
 * @author Stephane Nicoll
 */
class TestableAsyncUncaughtExceptionHandler
		implements AsyncUncaughtExceptionHandler {

	private final CountDownLatch latch = new CountDownLatch(1);

	private UncaughtExceptionDescriptor descriptor;

	private final boolean throwUnexpectedException;

	TestableAsyncUncaughtExceptionHandler() {
		this(false);
	}

	TestableAsyncUncaughtExceptionHandler(boolean throwUnexpectedException) {
		this.throwUnexpectedException = throwUnexpectedException;
	}

	@Override
	public void handleUncaughtException(Throwable ex, Method method, Object... params) {
		descriptor = new UncaughtExceptionDescriptor(ex, method);
		this.latch.countDown();
		if (throwUnexpectedException) {
			throw new IllegalStateException("Test exception");
		}
	}

	public boolean isCalled() {
		return descriptor != null;
	}

	public void assertCalledWith(Method expectedMethod, Class<? extends Throwable> expectedExceptionType) {
		assertThat(descriptor).as("Handler not called").isNotNull();
		assertThat(descriptor.ex.getClass()).as("Wrong exception type").isEqualTo(expectedExceptionType);
		assertThat(descriptor.method).as("Wrong method").isEqualTo(expectedMethod);
	}

	public void await(long timeout) {
		try {
			this.latch.await(timeout, TimeUnit.MILLISECONDS);
		}
		catch (Exception e) {
			Thread.currentThread().interrupt();
		}
	}

	private static class UncaughtExceptionDescriptor {
		private final Throwable ex;

		private final Method method;

		private UncaughtExceptionDescriptor(Throwable ex, Method method) {
			this.ex = ex;
			this.method = method;
		}
	}
}
