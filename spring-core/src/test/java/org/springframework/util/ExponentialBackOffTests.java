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

package org.springframework.util;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.util.backoff.BackOffExecution;
import org.springframework.util.backoff.ExponentialBackOff;

import static org.junit.Assert.*;

/**
 *
 * @author Stephane Nicoll
 */
public class ExponentialBackOffTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	@Test
	public void defaultInstance() {
		ExponentialBackOff backOff = new ExponentialBackOff();
		BackOffExecution execution = backOff.start();
		assertEquals(2000L, execution.nextBackOff());
		assertEquals(3000L, execution.nextBackOff());
		assertEquals(4500L, execution.nextBackOff());
	}

	@Test
	public void simpleIncrease() {
		ExponentialBackOff backOff = new ExponentialBackOff(100L, 2.0);
		BackOffExecution execution = backOff.start();
		assertEquals(100L, execution.nextBackOff());
		assertEquals(200L, execution.nextBackOff());
		assertEquals(400L, execution.nextBackOff());
		assertEquals(800L, execution.nextBackOff());
	}

	@Test
	public void fixedIncrease() {
		ExponentialBackOff backOff = new ExponentialBackOff(100L, 1.0);
		backOff.setMaxElapsedTime(300L);

		BackOffExecution execution = backOff.start();
		assertEquals(100L, execution.nextBackOff());
		assertEquals(100L, execution.nextBackOff());
		assertEquals(100L, execution.nextBackOff());
		assertEquals(BackOffExecution.STOP, execution.nextBackOff());
	}

	@Test
	public void maxIntervalReached() {
		ExponentialBackOff backOff = new ExponentialBackOff(2000L, 2.0);
		backOff.setMaxInterval(4000L);

		BackOffExecution execution = backOff.start();
		assertEquals(2000L, execution.nextBackOff());
		assertEquals(4000L, execution.nextBackOff());
		assertEquals(4000L, execution.nextBackOff()); // max reached
		assertEquals(4000L, execution.nextBackOff());
	}

	@Test
	public void maxAttemptsReached() {
		ExponentialBackOff backOff = new ExponentialBackOff(2000L, 2.0);
		backOff.setMaxElapsedTime(4000L);

		BackOffExecution execution = backOff.start();
		assertEquals(2000L, execution.nextBackOff());
		assertEquals(4000L, execution.nextBackOff());
		assertEquals(BackOffExecution.STOP, execution.nextBackOff()); // > 4 sec wait in total
	}

	@Test
	public void startReturnDifferentInstances() {
		ExponentialBackOff backOff = new ExponentialBackOff();
		backOff.setInitialInterval(2000L);
		backOff.setMultiplier(2.0);
		backOff.setMaxElapsedTime(4000L);

		BackOffExecution execution = backOff.start();
		BackOffExecution execution2 = backOff.start();

		assertEquals(2000L, execution.nextBackOff());
		assertEquals(2000L, execution2.nextBackOff());
		assertEquals(4000L, execution.nextBackOff());
		assertEquals(4000L, execution2.nextBackOff());
		assertEquals(BackOffExecution.STOP, execution.nextBackOff());
		assertEquals(BackOffExecution.STOP, execution2.nextBackOff());
	}

	@Test
	public void invalidInterval() {
		ExponentialBackOff backOff = new ExponentialBackOff();

		thrown.expect(IllegalArgumentException.class);
		backOff.setMultiplier(0.9);
	}

	@Test
	public void maxIntervalReachedImmediately() {
		ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
		backOff.setMaxInterval(50L);

		BackOffExecution execution = backOff.start();
		assertEquals(50L, execution.nextBackOff());
		assertEquals(50L, execution.nextBackOff());
	}

	@Test
	public void toStringContent() {
		ExponentialBackOff backOff = new ExponentialBackOff(2000L, 2.0);
		BackOffExecution execution = backOff.start();
		assertEquals("ExponentialBackOff{currentInterval=n/a, multiplier=2.0}", execution.toString());
		execution.nextBackOff();
		assertEquals("ExponentialBackOff{currentInterval=2000ms, multiplier=2.0}", execution.toString());
		execution.nextBackOff();
		assertEquals("ExponentialBackOff{currentInterval=4000ms, multiplier=2.0}", execution.toString());
	}

}
