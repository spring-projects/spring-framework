/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.web.servlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Test fixture for {@link FlashMap} tests.
 * 
 * @author Rossen Stoyanchev
 */
public class FlashMapTests {

	@Test
	public void isExpired() throws InterruptedException {
		FlashMap flashMap = new FlashMap();
		flashMap.startExpirationPeriod(0);

		Thread.sleep(1);

		assertTrue(flashMap.isExpired());
	}

	@Test
	public void notExpired() throws InterruptedException {
		assertFalse(new FlashMap().isExpired());

		FlashMap flashMap = new FlashMap();
		flashMap.startExpirationPeriod(10);
		Thread.sleep(100);

		assertFalse(flashMap.isExpired());
	}

	@Test
	public void compareTo() {
		FlashMap flashMap1 = new FlashMap();
		FlashMap flashMap2 = new FlashMap();
		assertEquals(0, flashMap1.compareTo(flashMap2));

		flashMap1.setTargetRequestPath("/path1");
		assertEquals(-1, flashMap1.compareTo(flashMap2));
		assertEquals(1, flashMap2.compareTo(flashMap1));

		flashMap2.setTargetRequestPath("/path2");
		assertEquals(0, flashMap1.compareTo(flashMap2));

		flashMap1.addTargetRequestParam("id", "1");
		assertEquals(-1, flashMap1.compareTo(flashMap2));
		assertEquals(1, flashMap2.compareTo(flashMap1));

		flashMap2.addTargetRequestParam("id", "2");
		assertEquals(0, flashMap1.compareTo(flashMap2));
	}

}
