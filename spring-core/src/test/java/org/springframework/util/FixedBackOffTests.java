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

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * @author Stephane Nicoll
 */
public class FixedBackOffTests {

	@Test
	public void defaultInstance() {
		FixedBackOff backOff = new FixedBackOff();
		for (int i = 0; i < 100; i++) {
			assertEquals(FixedBackOff.DEFAULT_INTERVAL, backOff.nextBackOff());
		}
	}

	@Test
	public void noAttemptAtAll() {
		FixedBackOff backOff = new FixedBackOff(100L, 0L);
		assertEquals(BackOff.STOP, backOff.nextBackOff());
	}

	@Test
	public void maxAttemptsReached() {
		FixedBackOff backOff = new FixedBackOff(200L, 2);
		assertEquals(200l, backOff.nextBackOff());
		assertEquals(200l, backOff.nextBackOff());
		assertEquals(BackOff.STOP, backOff.nextBackOff());
	}

	@Test
	public void resetOnInstance() {
		FixedBackOff backOff = new FixedBackOff(100L, 1);
		assertEquals(100l, backOff.nextBackOff());
		assertEquals(BackOff.STOP, backOff.nextBackOff());

		backOff.reset();

		assertEquals(100l, backOff.nextBackOff());
		assertEquals(BackOff.STOP, backOff.nextBackOff());
	}

	@Test
	public void liveUpdate() {
		FixedBackOff backOff = new FixedBackOff(100L, 1);
		assertEquals(100l, backOff.nextBackOff());

		backOff.setInterval(200l);
		backOff.setMaxAttempts(2);

		assertEquals(200l, backOff.nextBackOff());
		assertEquals(BackOff.STOP, backOff.nextBackOff());
	}

	@Test
	public void toStringContent() {
		FixedBackOff backOff = new FixedBackOff(200L, 10);
		assertEquals("FixedBackOff{interval=200, currentAttempts=0, maxAttempts=10}", backOff.toString());
		backOff.nextBackOff();
		assertEquals("FixedBackOff{interval=200, currentAttempts=1, maxAttempts=10}", backOff.toString());
		backOff.nextBackOff();
		assertEquals("FixedBackOff{interval=200, currentAttempts=2, maxAttempts=10}", backOff.toString());
	}

}
