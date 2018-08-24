/*
 * Copyright 2002-2018 the original author or authors.
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
package org.springframework.util.asyncassert;

import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

/**
 * Created on 23.08.2018.
 *
 * @author Korovin Anatoliy
 */
public class AsyncAssertTest {


	@Test
	public void testAwait() {
		// Arrange
		AtomicInteger variable = new AtomicInteger(0);
		// Act
		asyncIncrement(variable, 50);
		// Assert
		AsyncAssert.get()
				   .polling(10, TimeUnit.MILLISECONDS)
				   .timeout(1, TimeUnit.SECONDS)
				   .await(() -> variable.get() == 1);
		assertEquals(variable.get(), 1);
	}


	@Test(expected = AsyncAssertTimeoutException.class)
	public void testAwaitFail() {
		// Arrange
		AtomicInteger variable = new AtomicInteger(0);
		// Assert
		AsyncAssert.get()
				   .polling(10, TimeUnit.MILLISECONDS)
				   .timeout(1, TimeUnit.SECONDS)
				   .await(() -> variable.get() == 1);
	}

	@Test(expected = AsyncAssertTimeoutException.class)
	public void testAwaitWithTimeoutException() {
		// Arrange
		AtomicInteger variable = new AtomicInteger(0);
		// Act
		asyncIncrement(variable, 50);
		// Assert
		AsyncAssert.get()
				   .polling(10, TimeUnit.MILLISECONDS)
				   .timeout(15, TimeUnit.MILLISECONDS)
				   .await(() -> variable.get() == 1);
	}

	@Test(expected = AsyncAssertInternalException.class)
	public void testWithoutTimeout() throws Exception {
		AsyncAssert.get()
				   .await(() -> true);
	}

	@Test
	public void testWithAssert() throws Exception {
		// Arrange
		AtomicInteger variable = new AtomicInteger(0);
		// Act
		asyncIncrement(variable, 50);
		// Assert
		AsyncAssert.get()
				   .timeout(1, TimeUnit.SECONDS)
				   .await(() -> assertEquals(variable.get(), 1));
		assertEquals(variable.get(), 1);
	}

	@Test(expected = AsyncAssertTimeoutException.class)
	public void testWithAssertWithTimeout() throws Exception {
		// Arrange
		AtomicInteger variable = new AtomicInteger(0);
		// Act
		asyncIncrement(variable, 100);
		// Assert
		AsyncAssert.get()
				   .polling(10, TimeUnit.MILLISECONDS)
				   .timeout(50, TimeUnit.MILLISECONDS)
				   .await(() -> assertEquals(variable.get(), 1));
	}

	@Test(expected = AsyncAssertTimeoutException.class)
	public void testWithAssertWithFail() throws Exception {
		// Arrange
		AtomicInteger variable = new AtomicInteger(0);
		// Assert
		AsyncAssert.get()
				   .polling(10, TimeUnit.MILLISECONDS)
				   .timeout(50, TimeUnit.MILLISECONDS)
				   .await(() -> assertEquals(variable.get(), 1));
	}

	private void asyncIncrement(AtomicInteger variable, long timeout) {
		new Thread(() -> {
			try {
				Thread.sleep(timeout);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			variable.incrementAndGet();
		}).start();
	}
}
