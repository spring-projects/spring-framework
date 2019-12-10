/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.scheduling.annotation;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;

import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Juergen Hoeller
 */
public class AsyncResultTests {

	@Test
	public void asyncResultWithCallbackAndValue() throws Exception {
		String value = "val";
		final Set<String> values = new HashSet<>(1);
		ListenableFuture<String> future = AsyncResult.forValue(value);
		future.addCallback(new ListenableFutureCallback<String>() {
			@Override
			public void onSuccess(String result) {
				values.add(result);
			}
			@Override
			public void onFailure(Throwable ex) {
				throw new AssertionError("Failure callback not expected: " + ex, ex);
			}
		});
		assertThat(values.iterator().next()).isSameAs(value);
		assertThat(future.get()).isSameAs(value);
		assertThat(future.completable().get()).isSameAs(value);
		future.completable().thenAccept(v -> assertThat(v).isSameAs(value));
	}

	@Test
	public void asyncResultWithCallbackAndException() throws Exception {
		IOException ex = new IOException();
		final Set<Throwable> values = new HashSet<>(1);
		ListenableFuture<String> future = AsyncResult.forExecutionException(ex);
		future.addCallback(new ListenableFutureCallback<String>() {
			@Override
			public void onSuccess(String result) {
				throw new AssertionError("Success callback not expected: " + result);
			}
			@Override
			public void onFailure(Throwable ex) {
				values.add(ex);
			}
		});
		assertThat(values.iterator().next()).isSameAs(ex);
		assertThatExceptionOfType(ExecutionException.class).isThrownBy(
				future::get)
			.withCause(ex);
		assertThatExceptionOfType(ExecutionException.class).isThrownBy(
				future.completable()::get)
			.withCause(ex);
	}

	@Test
	public void asyncResultWithSeparateCallbacksAndValue() throws Exception {
		String value = "val";
		final Set<String> values = new HashSet<>(1);
		ListenableFuture<String> future = AsyncResult.forValue(value);
		future.addCallback(values::add, ex -> new AssertionError("Failure callback not expected: " + ex));
		assertThat(values.iterator().next()).isSameAs(value);
		assertThat(future.get()).isSameAs(value);
		assertThat(future.completable().get()).isSameAs(value);
		future.completable().thenAccept(v -> assertThat(v).isSameAs(value));
	}

	@Test
	public void asyncResultWithSeparateCallbacksAndException() throws Exception {
		IOException ex = new IOException();
		final Set<Throwable> values = new HashSet<>(1);
		ListenableFuture<String> future = AsyncResult.forExecutionException(ex);
		future.addCallback(result -> new AssertionError("Success callback not expected: " + result), values::add);
		assertThat(values.iterator().next()).isSameAs(ex);
		assertThatExceptionOfType(ExecutionException.class).isThrownBy(
				future::get)
			.withCause(ex);
		assertThatExceptionOfType(ExecutionException.class).isThrownBy(
				future.completable()::get)
			.withCause(ex);
	}

}
