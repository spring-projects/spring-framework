/*
 * Copyright 2002-2013 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Test;

/**
 * @author Arjen Poutsma
 */
public class ListenableFutureTaskTests {

	@Test
	public void success() throws ExecutionException, InterruptedException {
		final String s = "Hello World";
		Callable<String> callable = new Callable<String>() {
			@Override
			public String call() throws Exception {
				return s;
			}
		};
		ListenableFutureTask<String> task = new ListenableFutureTask<String>(callable);
		task.addCallback(new ListenableFutureCallback<String>() {
			@Override
			public void onSuccess(String result) {
				assertEquals(s, result);
			}

			@Override
			public void onFailure(Throwable t) {
				fail(t.getMessage());
			}
		});
		task.run();
	}

	@Test
	public void failure() throws ExecutionException, InterruptedException {
		final String s = "Hello World";
		Callable<String> callable = new Callable<String>() {
			@Override
			public String call() throws Exception {
				throw new IOException(s);
			}
		};
		ListenableFutureTask<String> task = new ListenableFutureTask<String>(callable);
		task.addCallback(new ListenableFutureCallback<String>() {
			@Override
			public void onSuccess(String result) {
				fail("onSuccess not expected");
			}

			@Override
			public void onFailure(Throwable t) {
				assertEquals(s, t.getMessage());
			}
		});
		task.run();
	}



}
