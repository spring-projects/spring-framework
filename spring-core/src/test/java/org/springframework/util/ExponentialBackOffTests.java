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

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

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
		assertEquals(2000l, backOff.nextBackOff());
		assertEquals(3000l, backOff.nextBackOff());
		assertEquals(4500l, backOff.nextBackOff());
	}

	@Test
	public void simpleIncrease() {
		ExponentialBackOff backOff = new ExponentialBackOff(100L, 2.0);
		assertEquals(100l, backOff.nextBackOff());
		assertEquals(200l, backOff.nextBackOff());
		assertEquals(400l, backOff.nextBackOff());
		assertEquals(800l, backOff.nextBackOff());
	}

	@Test
	public void fixedIncrease() {
		ExponentialBackOff backOff = new ExponentialBackOff(100L, 1.0);
		backOff.setMaxElapsedTime(300l);
		assertEquals(100l, backOff.nextBackOff());
		assertEquals(100l, backOff.nextBackOff());
		assertEquals(100l, backOff.nextBackOff());
		assertEquals(BackOff.STOP, backOff.nextBackOff());
	}

	@Test
	public void maxIntervalReached() {
		ExponentialBackOff backOff = new ExponentialBackOff(2000L, 2.0);
		backOff.setMaxInterval(4000L);
		assertEquals(2000l, backOff.nextBackOff());
		assertEquals(4000l, backOff.nextBackOff());
		assertEquals(4000l, backOff.nextBackOff()); // max reached
		assertEquals(4000l, backOff.nextBackOff());
	}

	@Test
	public void maxAttemptsReached() {
		ExponentialBackOff backOff = new ExponentialBackOff(2000L, 2.0);
		backOff.setMaxElapsedTime(4000L);
		assertEquals(2000l, backOff.nextBackOff());
		assertEquals(4000l, backOff.nextBackOff());
		assertEquals(BackOff.STOP, backOff.nextBackOff()); // > 4 sec wait in total
	}

	@Test
	public void resetInstance() {
		ExponentialBackOff backOff = new ExponentialBackOff();
		backOff.setInitialInterval(2000L);
		backOff.setMultiplier(2.0);
		backOff.setMaxElapsedTime(4000L);
		assertEquals(2000l, backOff.nextBackOff());
		assertEquals(4000l, backOff.nextBackOff());
		assertEquals(BackOff.STOP, backOff.nextBackOff());

		backOff.reset();

		assertEquals(2000l, backOff.nextBackOff());
		assertEquals(4000l, backOff.nextBackOff());
		assertEquals(BackOff.STOP, backOff.nextBackOff());
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

		assertEquals(50L, backOff.nextBackOff());
		assertEquals(50L, backOff.nextBackOff());
	}

	@Test
	public void toStringContent() {
		ExponentialBackOff backOff = new ExponentialBackOff(2000L, 2.0);
		assertEquals("ExponentialBackOff{currentInterval=n/a, multiplier=2.0}", backOff.toString());
		backOff.nextBackOff();
		assertEquals("ExponentialBackOff{currentInterval=2000ms, multiplier=2.0}", backOff.toString());
		backOff.nextBackOff();
		assertEquals("ExponentialBackOff{currentInterval=4000ms, multiplier=2.0}", backOff.toString());
	}

}
