/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.scheduling.support;

import java.util.Date;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Juergen Hoeller
 * @author Ruslan Sibgatullin
 */
@SuppressWarnings("deprecation")
public class CronSequenceGeneratorTests {

	@Test
	public void at50Seconds() {
		assertEquals(new Date(2012, 6, 2, 1, 0),
				new CronSequenceGenerator("*/15 * 1-4 * * *").next(new Date(2012, 6, 1, 9, 53, 50)));
	}

	@Test
	public void at0Seconds() {
		assertEquals(new Date(2012, 6, 2, 1, 0),
				new CronSequenceGenerator("*/15 * 1-4 * * *").next(new Date(2012, 6, 1, 9, 53)));
	}

	@Test
	public void at0Minutes() {
		assertEquals(new Date(2012, 6, 2, 1, 0),
				new CronSequenceGenerator("0 */2 1-4 * * *").next(new Date(2012, 6, 1, 9, 0)));
	}

	@Test(expected = IllegalArgumentException.class)
	public void with0Increment() {
		new CronSequenceGenerator("*/0 * * * * *").next(new Date(2012, 6, 1, 9, 0));
	}

	@Test(expected = IllegalArgumentException.class)
	public void withNegativeIncrement() {
		new CronSequenceGenerator("*/-1 * * * * *").next(new Date(2012, 6, 1, 9, 0));
	}

	@Test(expected = IllegalArgumentException.class)
	public void withInvertedMinuteRange() {
		new CronSequenceGenerator("* 6-5 * * * *").next(new Date(2012, 6, 1, 9, 0));
	}

	@Test(expected = IllegalArgumentException.class)
	public void withInvertedHourRange() {
		new CronSequenceGenerator("* * 6-5 * * *").next(new Date(2012, 6, 1, 9, 0));
	}

	@Test
	public void withSameMinuteRange() {
		new CronSequenceGenerator("* 6-6 * * * *").next(new Date(2012, 6, 1, 9, 0));
	}

	@Test
	public void withSameHourRange() {
		new CronSequenceGenerator("* * 6-6 * * *").next(new Date(2012, 6, 1, 9, 0));
	}

	@Test
	public void validExpression() {
		assertTrue(CronSequenceGenerator.isValidExpression("0 */2 1-4 * * *"));
	}

	@Test
	public void invalidExpressionWithLength() {
		assertFalse(CronSequenceGenerator.isValidExpression("0 */2 1-4 * * * *"));
	}

	@Test
	public void invalidExpressionWithSeconds() {
		assertFalse(CronSequenceGenerator.isValidExpression("100 */2 1-4 * * *"));
	}

	@Test
	public void invalidExpressionWithMonths() {
		assertFalse(CronSequenceGenerator.isValidExpression("0 */2 1-4 * INVALID *"));
	}

	@Test
	public void nullExpression() {
		assertFalse(CronSequenceGenerator.isValidExpression(null));
	}

}
