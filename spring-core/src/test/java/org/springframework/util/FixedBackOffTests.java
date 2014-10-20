/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.util;

import org.junit.Test;

import org.springframework.util.backoff.BackOffExecution;
import org.springframework.util.backoff.FixedBackOff;

import static org.junit.Assert.*;

/**
 * @author Stephane Nicoll
 */
public class FixedBackOffTests {

	@Test
	public void defaultInstance() {
		FixedBackOff backOff = new FixedBackOff();
		BackOffExecution execution = backOff.start();
		for (int i = 0; i < 100; i++) {
			assertEquals(FixedBackOff.DEFAULT_INTERVAL, execution.nextBackOff());
		}
	}

	@Test
	public void noAttemptAtAll() {
		FixedBackOff backOff = new FixedBackOff(100L, 0L);
		BackOffExecution execution = backOff.start();
		assertEquals(BackOffExecution.STOP, execution.nextBackOff());
	}

	@Test
	public void maxAttemptsReached() {
		FixedBackOff backOff = new FixedBackOff(200L, 2);
		BackOffExecution execution = backOff.start();
		assertEquals(200l, execution.nextBackOff());
		assertEquals(200l, execution.nextBackOff());
		assertEquals(BackOffExecution.STOP, execution.nextBackOff());
	}

	@Test
	public void startReturnDifferentInstances() {
		FixedBackOff backOff = new FixedBackOff(100L, 1);
		BackOffExecution execution = backOff.start();
		BackOffExecution execution2 = backOff.start();

		assertEquals(100l, execution.nextBackOff());
		assertEquals(100l, execution2.nextBackOff());
		assertEquals(BackOffExecution.STOP, execution.nextBackOff());
		assertEquals(BackOffExecution.STOP, execution2.nextBackOff());
	}

	@Test
	public void liveUpdate() {
		FixedBackOff backOff = new FixedBackOff(100L, 1);
		BackOffExecution execution = backOff.start();
		assertEquals(100l, execution.nextBackOff());

		backOff.setInterval(200l);
		backOff.setMaxAttempts(2);

		assertEquals(200l, execution.nextBackOff());
		assertEquals(BackOffExecution.STOP, execution.nextBackOff());
	}

	@Test
	public void toStringContent() {
		FixedBackOff backOff = new FixedBackOff(200L, 10);
		BackOffExecution execution = backOff.start();
		assertEquals("FixedBackOff{interval=200, currentAttempts=0, maxAttempts=10}", execution.toString());
		execution.nextBackOff();
		assertEquals("FixedBackOff{interval=200, currentAttempts=1, maxAttempts=10}", execution.toString());
		execution.nextBackOff();
		assertEquals("FixedBackOff{interval=200, currentAttempts=2, maxAttempts=10}", execution.toString());
	}

}
