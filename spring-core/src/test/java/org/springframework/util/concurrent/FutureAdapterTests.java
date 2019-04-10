/*
 * Copyright 2002-2013 the original author or authors.
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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * @author Arjen Poutsma
 */
public class FutureAdapterTests {

	private FutureAdapter<String, Integer> adapter;

	private Future<Integer> adaptee;


	@Before
	@SuppressWarnings("unchecked")
	public void setUp() {
		adaptee = mock(Future.class);
		adapter = new FutureAdapter<String, Integer>(adaptee) {
			@Override
			protected String adapt(Integer adapteeResult) throws ExecutionException {
				return adapteeResult.toString();
			}
		};
	}

	@Test
	public void cancel() throws Exception {
		given(adaptee.cancel(true)).willReturn(true);
		boolean result = adapter.cancel(true);
		assertTrue(result);
	}

	@Test
	public void isCancelled() {
		given(adaptee.isCancelled()).willReturn(true);
		boolean result = adapter.isCancelled();
		assertTrue(result);
	}

	@Test
	public void isDone() {
		given(adaptee.isDone()).willReturn(true);
		boolean result = adapter.isDone();
		assertTrue(result);
	}

	@Test
	public void get() throws Exception {
		given(adaptee.get()).willReturn(42);
		String result = adapter.get();
		assertEquals("42", result);
	}

	@Test
	public void getTimeOut() throws Exception {
		given(adaptee.get(1, TimeUnit.SECONDS)).willReturn(42);
		String result = adapter.get(1, TimeUnit.SECONDS);
		assertEquals("42", result);
	}


}
