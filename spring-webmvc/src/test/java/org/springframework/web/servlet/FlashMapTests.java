/*
 * Copyright 2002-2012 the original author or authors.
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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Test fixture for {@link FlashMap} tests.
 *
 * @author Rossen Stoyanchev
 */
public class FlashMapTests {

	@Test
	public void isExpired() throws InterruptedException {
		assertFalse(new FlashMap().isExpired());

		FlashMap flashMap = new FlashMap();
		flashMap.startExpirationPeriod(0);
		Thread.sleep(100);

		assertTrue(flashMap.isExpired());
	}

	@Test
	public void notExpired() throws InterruptedException {
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

	@Test
	public void addTargetRequestParamNullValue() {
		FlashMap flashMap = new FlashMap();
		flashMap.addTargetRequestParam("text", "abc");
		flashMap.addTargetRequestParam("empty", " ");
		flashMap.addTargetRequestParam("null", null);

		assertEquals(1, flashMap.getTargetRequestParams().size());
		assertEquals("abc", flashMap.getTargetRequestParams().getFirst("text"));
	}

	@Test
	public void addTargetRequestParamsNullValue() {
		MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
		params.add("key", "abc");
		params.add("key", " ");
		params.add("key", null);

		FlashMap flashMap = new FlashMap();
		flashMap.addTargetRequestParams(params);

		assertEquals(1, flashMap.getTargetRequestParams().size());
		assertEquals(1, flashMap.getTargetRequestParams().get("key").size());
		assertEquals("abc", flashMap.getTargetRequestParams().getFirst("key"));
	}

	@Test
	public void addTargetRequestParamNullKey() {
		FlashMap flashMap = new FlashMap();
		flashMap.addTargetRequestParam(" ", "abc");
		flashMap.addTargetRequestParam(null, "abc");

		assertTrue(flashMap.getTargetRequestParams().isEmpty());
	}

	@Test
	public void addTargetRequestParamsNullKey() {
		MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
		params.add(" ", "abc");
		params.add(null, " ");

		FlashMap flashMap = new FlashMap();
		flashMap.addTargetRequestParams(params);

		assertTrue(flashMap.getTargetRequestParams().isEmpty());
	}

}
