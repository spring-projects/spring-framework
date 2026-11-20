/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.interceptor;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.core.task.AsyncTaskExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link AsyncExecutionInterceptor}.
 *
 * @author Bao Ngo
 */
class AsyncExecutionInterceptorTests {

	@Test
	@SuppressWarnings("unchecked")
	void invokeOnInterfaceWithGeneric() throws Throwable {
		AsyncExecutionInterceptor interceptor = spy(new AsyncExecutionInterceptor(null));
		FutureRunner impl = new FutureRunner();
		MethodInvocation mi = mock();
		given(mi.getThis()).willReturn(impl);
		given(mi.getMethod()).willReturn(GenericRunner.class.getMethod("run"));

		interceptor.invoke(mi);
		ArgumentCaptor<Class<?>> classArgumentCaptor = ArgumentCaptor.forClass(Class.class);
		verify(interceptor).doSubmit(any(Callable.class), any(AsyncTaskExecutor.class), classArgumentCaptor.capture());
		assertThat(classArgumentCaptor.getValue()).isEqualTo(Future.class);
	}


	interface GenericRunner<O> {

		O run();
	}


	static class FutureRunner implements GenericRunner<Future<Void>> {

		@Override
		public Future<Void> run() {
			return CompletableFuture.runAsync(() -> {});
		}
	}

}
