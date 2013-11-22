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

package org.springframework.scheduling.support;

import java.util.Date;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Juergen Hoeller
 */
@SuppressWarnings("deprecation")
public class CronSequenceGeneratorTests {

	@Test
	public void testAt50Seconds() {
		assertEquals(new Date(2012, 6, 2, 1, 0),
				new CronSequenceGenerator("*/15 * 1-4 * * *").next(new Date(2012, 6, 1, 9, 53, 50)));
	}

	@Test
	public void testAt0Seconds() {
		assertEquals(new Date(2012, 6, 2, 1, 0),
				new CronSequenceGenerator("*/15 * 1-4 * * *").next(new Date(2012, 6, 1, 9, 53)));
	}

	@Test
	public void testAt0Minutes() {
		assertEquals(new Date(2012, 6, 2, 1, 0),
				new CronSequenceGenerator("0 */2 1-4 * * *").next(new Date(2012, 6, 1, 9, 0)));
	}

}
