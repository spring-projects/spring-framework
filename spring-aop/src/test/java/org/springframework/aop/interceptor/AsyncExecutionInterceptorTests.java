/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.aop.interceptor;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.core.task.AsyncTaskExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;


/**
 * Tests for {@link AsyncExecutionInterceptor}.
 *
 * @author Bao Ngo
 * @since 7.0
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
			return CompletableFuture.runAsync(() -> {
			});
		}
	}
}
