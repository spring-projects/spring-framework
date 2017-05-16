/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.scheduling.annotation;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import static org.junit.Assert.*;

/**
 * @author Juergen Hoeller
 */
public class AsyncResultTests {

	@Test
	public void asyncResultWithCallbackAndValue() {
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
				fail("Failure callback not expected: " + ex);
			}
		});
		assertSame(value, values.iterator().next());
	}

	@Test
	public void asyncResultWithCallbackAndException() {
		IOException ex = new IOException();
		final Set<Throwable> values = new HashSet<>(1);
		ListenableFuture<String> future = AsyncResult.forExecutionException(ex);
		future.addCallback(new ListenableFutureCallback<String>() {
			@Override
			public void onSuccess(String result) {
				fail("Success callback not expected: " + result);
			}
			@Override
			public void onFailure(Throwable ex) {
				values.add(ex);
			}
		});
		assertSame(ex, values.iterator().next());
	}

	@Test
	public void asyncResultWithSeparateCallbacksAndValue() {
		String value = "val";
		final Set<String> values = new HashSet<>(1);
		ListenableFuture<String> future = AsyncResult.forValue(value);
		future.addCallback(values::add, (ex) -> fail("Failure callback not expected: " + ex));
		assertSame(value, values.iterator().next());
	}

	@Test
	public void asyncResultWithSeparateCallbacksAndException() {
		IOException ex = new IOException();
		final Set<Throwable> values = new HashSet<>(1);
		ListenableFuture<String> future = AsyncResult.forExecutionException(ex);
		future.addCallback((result) -> fail("Success callback not expected: " + result), values::add);
		assertSame(ex, values.iterator().next());
	}

}
