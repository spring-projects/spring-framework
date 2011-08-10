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

package org.springframework.web.servlet.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ModelMap;
import org.springframework.web.servlet.FlashMap;

/**
 * Test fixture for {@link FlashMap} tests.
 * 
 * @author Rossen Stoyanchev
 */
public class FlashMapTests {

	@Test
	public void matchAnyUrlPath() {
		FlashMap flashMap = new FlashMap();

		assertTrue(flashMap.matches(new MockHttpServletRequest("GET", "")));
		assertTrue(flashMap.matches(new MockHttpServletRequest("GET", "/")));
		assertTrue(flashMap.matches(new MockHttpServletRequest("GET", "/yes")));
	}

	@Test
	public void matchUrlPath() {
		FlashMap flashMap = new FlashMap();
		flashMap.setExpectedUrlPath(null, "/yes");

		assertTrue(flashMap.matches(new MockHttpServletRequest("GET", "/yes")));
		assertTrue(flashMap.matches(new MockHttpServletRequest("GET", "/yes/")));
		assertFalse(flashMap.matches(new MockHttpServletRequest("GET", "/yes/but")));
		assertFalse(flashMap.matches(new MockHttpServletRequest("GET", "/no")));

		flashMap.setExpectedUrlPath(null, "/thats it?");

		assertTrue(flashMap.matches(new MockHttpServletRequest("GET", "/thats it")));
		assertTrue(flashMap.matches(new MockHttpServletRequest("GET", "/thats%20it")));
	}

	@Test
	public void matchRelativeUrlPath() {
		FlashMap flashMap = new FlashMap();

		flashMap.setExpectedUrlPath(new MockHttpServletRequest("GET", "/oh/no"), "yes");
		assertTrue(flashMap.matches(new MockHttpServletRequest("GET", "/oh/yes")));

		flashMap.setExpectedUrlPath(new MockHttpServletRequest("GET", "/oh/not/again"), "../ok");
		assertTrue(flashMap.matches(new MockHttpServletRequest("GET", "/oh/ok")));

		flashMap.setExpectedUrlPath(new MockHttpServletRequest("GET", "/yes/it/is"), "..");
		assertTrue(flashMap.matches(new MockHttpServletRequest("GET", "/yes")));

		flashMap.setExpectedUrlPath(new MockHttpServletRequest("GET", "/yes/it/is"), "../");
		assertTrue(flashMap.matches(new MockHttpServletRequest("GET", "/yes")));

		flashMap.setExpectedUrlPath(new MockHttpServletRequest("GET", "/thats it/really"), "./");
		assertTrue(flashMap.matches(new MockHttpServletRequest("GET", "/thats%20it")));
	}

	@Test
	public void matchAbsoluteUrlPath() {
		FlashMap flashMap = new FlashMap();
		flashMap.setExpectedUrlPath(new MockHttpServletRequest(), "http://example.com");

		assertTrue(flashMap.matches(new MockHttpServletRequest("GET", "")));
		assertTrue(flashMap.matches(new MockHttpServletRequest("GET", "/")));
		assertFalse(flashMap.matches(new MockHttpServletRequest("GET", "/no")));

		flashMap.setExpectedUrlPath(null, "http://example.com/");

		assertTrue(flashMap.matches(new MockHttpServletRequest("GET", "")));
		assertTrue(flashMap.matches(new MockHttpServletRequest("GET", "/")));
		assertFalse(flashMap.matches(new MockHttpServletRequest("GET", "/no")));

		flashMap.setExpectedUrlPath(null, "http://example.com/yes");

		assertTrue(flashMap.matches(new MockHttpServletRequest("GET", "/yes")));
		assertTrue(flashMap.matches(new MockHttpServletRequest("GET", "/yes/")));
		assertFalse(flashMap.matches(new MockHttpServletRequest("GET", "/no")));
		assertFalse(flashMap.matches(new MockHttpServletRequest("GET", "/yes/no")));

		flashMap.setExpectedUrlPath(null, "http://example.com/yes?a=1");

		assertTrue(flashMap.matches(new MockHttpServletRequest("GET", "/yes")));
	}

	@Test
	public void matchExpectedRequestParameter() {
		String parameterName = "numero";

		FlashMap flashMap = new FlashMap();
		flashMap.setExpectedRequestParameters(new ModelMap(parameterName, "uno"));

		MockHttpServletRequest request = new MockHttpServletRequest();
		assertFalse(flashMap.matches(request));

		request.setParameter(parameterName, "uno");
		assertTrue(flashMap.matches(request));

		request.setParameter(parameterName, "dos");
		assertFalse(flashMap.matches(request));

		request.setParameter(parameterName, (String) null);
		assertFalse(flashMap.matches(request));
	}

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

		flashMap1.setExpectedUrlPath(null, "/path1");
		assertEquals(-1, flashMap1.compareTo(flashMap2));
		assertEquals(1, flashMap2.compareTo(flashMap1));

		flashMap2.setExpectedUrlPath(null, "/path2");
		assertEquals(0, flashMap1.compareTo(flashMap2));

		flashMap1.setExpectedRequestParameters(new ModelMap("id", "1"));
		assertEquals(-1, flashMap1.compareTo(flashMap2));
		assertEquals(1, flashMap2.compareTo(flashMap1));

		flashMap2.setExpectedRequestParameters(new ModelMap("id", "2"));
		assertEquals(0, flashMap1.compareTo(flashMap2));
	}

}
