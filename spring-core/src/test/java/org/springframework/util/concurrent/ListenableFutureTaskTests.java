/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.util.concurrent;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 */
@SuppressWarnings({ "unchecked", "deprecation" })
class ListenableFutureTaskTests {

	@Test
	void success() throws Exception {
		final String s = "Hello World";
		Callable<String> callable = () -> s;

		ListenableFutureTask<String> task = new ListenableFutureTask<>(callable);
		task.addCallback(new ListenableFutureCallback<String>() {
			@Override
			public void onSuccess(String result) {
				assertThat(result).isEqualTo(s);
			}
			@Override
			public void onFailure(Throwable ex) {
				throw new AssertionError(ex.getMessage(), ex);
			}
		});
		task.run();

		assertThat(task.get()).isSameAs(s);
		assertThat(task.completable().get()).isSameAs(s);
		task.completable().thenAccept(v -> assertThat(v).isSameAs(s));
	}

	@Test
	void failure() throws Exception {
		final String s = "Hello World";
		Callable<String> callable = () -> {
			throw new IOException(s);
		};

		ListenableFutureTask<String> task = new ListenableFutureTask<>(callable);
		task.addCallback(new ListenableFutureCallback<String>() {
			@Override
			public void onSuccess(String result) {
				fail("onSuccess not expected");
			}
			@Override
			public void onFailure(Throwable ex) {
				assertThat(ex.getMessage()).isEqualTo(s);
			}
		});
		task.run();

		assertThatExceptionOfType(ExecutionException.class)
			.isThrownBy(task::get)
			.havingCause()
			.withMessage(s);
		assertThatExceptionOfType(ExecutionException.class)
			.isThrownBy(task.completable()::get)
			.havingCause()
			.withMessage(s);
	}

	@Test
	void successWithLambdas() throws Exception {
		final String s = "Hello World";
		Callable<String> callable = () -> s;

		SuccessCallback<String> successCallback = mock();
		FailureCallback failureCallback = mock();
		ListenableFutureTask<String> task = new ListenableFutureTask<>(callable);
		task.addCallback(successCallback, failureCallback);
		task.run();
		verify(successCallback).onSuccess(s);
		verifyNoInteractions(failureCallback);

		assertThat(task.get()).isSameAs(s);
		assertThat(task.completable().get()).isSameAs(s);
		task.completable().thenAccept(v -> assertThat(v).isSameAs(s));
	}

	@Test
	void failureWithLambdas() throws Exception {
		final String s = "Hello World";
		IOException ex = new IOException(s);
		Callable<String> callable = () -> {
			throw ex;
		};

		SuccessCallback<String> successCallback = mock();
		FailureCallback failureCallback = mock();
		ListenableFutureTask<String> task = new ListenableFutureTask<>(callable);
		task.addCallback(successCallback, failureCallback);
		task.run();
		verify(failureCallback).onFailure(ex);
		verifyNoInteractions(successCallback);

		assertThatExceptionOfType(ExecutionException.class)
				.isThrownBy(task::get)
				.havingCause()
				.withMessage(s);
		assertThatExceptionOfType(ExecutionException.class)
				.isThrownBy(task.completable()::get)
				.havingCause()
				.withMessage(s);
	}

}
