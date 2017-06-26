/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 */
@SuppressWarnings("unchecked")
public class ListenableFutureTaskTests {

	@Test
	public void success() throws Exception {
		final String s = "Hello World";
		Callable<String> callable = () -> s;

		ListenableFutureTask<String> task = new ListenableFutureTask<>(callable);
		task.addCallback(new ListenableFutureCallback<String>() {
			@Override
			public void onSuccess(String result) {
				assertEquals(s, result);
			}
			@Override
			public void onFailure(Throwable ex) {
				fail(ex.getMessage());
			}
		});
		task.run();

		assertSame(s, task.get());
		assertSame(s, task.completable().get());
		task.completable().thenAccept(v -> assertSame(s, v));
	}

	@Test
	public void failure() throws Exception {
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
				assertEquals(s, ex.getMessage());
			}
		});
		task.run();

		try {
			task.get();
			fail("Should have thrown ExecutionException");
		}
		catch (ExecutionException ex) {
			assertSame(s, ex.getCause().getMessage());
		}
		try {
			task.completable().get();
			fail("Should have thrown ExecutionException");
		}
		catch (ExecutionException ex) {
			assertSame(s, ex.getCause().getMessage());
		}
	}

	@Test
	public void successWithLambdas() throws Exception {
		final String s = "Hello World";
		Callable<String> callable = () -> s;

		SuccessCallback<String> successCallback = mock(SuccessCallback.class);
		FailureCallback failureCallback = mock(FailureCallback.class);
		ListenableFutureTask<String> task = new ListenableFutureTask<>(callable);
		task.addCallback(successCallback, failureCallback);
		task.run();
		verify(successCallback).onSuccess(s);
		verifyZeroInteractions(failureCallback);

		assertSame(s, task.get());
		assertSame(s, task.completable().get());
		task.completable().thenAccept(v -> assertSame(s, v));
	}

	@Test
	public void failureWithLambdas() throws Exception {
		final String s = "Hello World";
		IOException ex = new IOException(s);
		Callable<String> callable = () -> {
			throw ex;
		};

		SuccessCallback<String> successCallback = mock(SuccessCallback.class);
		FailureCallback failureCallback = mock(FailureCallback.class);
		ListenableFutureTask<String> task = new ListenableFutureTask<>(callable);
		task.addCallback(successCallback, failureCallback);
		task.run();
		verify(failureCallback).onFailure(ex);
		verifyZeroInteractions(successCallback);

		try {
			task.get();
			fail("Should have thrown ExecutionException");
		}
		catch (ExecutionException ex2) {
			assertSame(s, ex2.getCause().getMessage());
		}
		try {
			task.completable().get();
			fail("Should have thrown ExecutionException");
		}
		catch (ExecutionException ex2) {
			assertSame(s, ex2.getCause().getMessage());
		}
	}

}
