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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.FlashMapManager;

/**
 * Test fixture for {@link DefaultFlashMapManager} tests.
 *
 * @author Rossen Stoyanchev
 */
public class DefaultFlashMapManagerTests {

	private DefaultFlashMapManager flashMapManager;

	private MockHttpServletRequest request;
	
	@Before
	public void setup() {
		this.flashMapManager = new DefaultFlashMapManager();
		this.request = new MockHttpServletRequest();
	}
	
	@Test
	public void requestAlreadyStarted() {
		request.setAttribute(FlashMapManager.CURRENT_FLASH_MAP_ATTRIBUTE, new FlashMap());
		boolean actual = this.flashMapManager.requestStarted(this.request);
		
		assertFalse(actual);
	}
	
	@Test
	public void createFlashMap() {
		boolean actual = this.flashMapManager.requestStarted(this.request);
		FlashMap flashMap = RequestContextUtils.getFlashMap(this.request);

		assertTrue(actual);
		assertNotNull(flashMap);
		assertNotNull(flashMap.getKey());
		assertEquals("_flashKey", flashMap.getKeyParameterName());
	}
	
	@Test
	public void createFlashMapWithoutKey() {
		this.flashMapManager.setUseUniqueFlashKey(false);
		boolean actual = this.flashMapManager.requestStarted(this.request);
		FlashMap flashMap = RequestContextUtils.getFlashMap(this.request);

		assertTrue(actual);
		assertNotNull(flashMap);
		assertNull(flashMap.getKey());
		assertNull(flashMap.getKeyParameterName());
	}

	@Test
	public void lookupPreviousFlashMap() {
		FlashMap flashMap = new FlashMap("key", "_flashKey");
		flashMap.put("name", "value");
		Map<String, FlashMap> allFlashMaps = new HashMap<String, FlashMap>();
		allFlashMaps.put(flashMap.getKey(), flashMap);
		
		this.request.getSession().setAttribute(DefaultFlashMapManager.FLASH_MAPS_SESSION_ATTRIBUTE, allFlashMaps);
		this.request.addParameter("_flashKey", flashMap.getKey());
		this.flashMapManager.requestStarted(this.request);
		
		assertSame(flashMap, request.getAttribute(DefaultFlashMapManager.PREVIOUS_FLASH_MAP_ATTRIBUTE));
		assertEquals("value", request.getAttribute("name"));
	}

	@Test
	public void lookupPreviousFlashMapWithoutKey() {
		Map<String, FlashMap> allFlashMaps = new HashMap<String, FlashMap>();
		request.getSession().setAttribute(DefaultFlashMapManager.FLASH_MAPS_SESSION_ATTRIBUTE, allFlashMaps);

		FlashMap flashMap = new FlashMap();
		flashMap.put("name", "value");
		allFlashMaps.put("key", flashMap);
		
		this.flashMapManager.setUseUniqueFlashKey(false);
		this.flashMapManager.requestStarted(this.request);
		
		assertSame(flashMap, this.request.getAttribute(DefaultFlashMapManager.PREVIOUS_FLASH_MAP_ATTRIBUTE));
		assertEquals("value", this.request.getAttribute("name"));
	}

	@SuppressWarnings("static-access")
	@Test
	public void removeExpired() throws InterruptedException {
		FlashMap[] flashMapArray = new FlashMap[5];
		flashMapArray[0] = new FlashMap("key0", "_flashKey");
		flashMapArray[1] = new FlashMap("key1", "_flashKey");
		flashMapArray[2] = new FlashMap("key2", "_flashKey");
		flashMapArray[3] = new FlashMap("key3", "_flashKey");
		flashMapArray[4] = new FlashMap("key4", "_flashKey");
		
		Map<String, FlashMap> allFlashMaps = new HashMap<String, FlashMap>();
		for (FlashMap flashMap : flashMapArray) {
			allFlashMaps.put(flashMap.getKey(), flashMap);
		}
		
		flashMapArray[1].startExpirationPeriod(0);
		flashMapArray[3].startExpirationPeriod(0);
		
		Thread.currentThread().sleep(5);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.getSession().setAttribute(DefaultFlashMapManager.FLASH_MAPS_SESSION_ATTRIBUTE, allFlashMaps);
		request.setParameter("_flashKey", "key0");
		this.flashMapManager.requestStarted(request);
		
		assertEquals(2, allFlashMaps.size());
		assertNotNull(allFlashMaps.get("key2"));
		assertNotNull(allFlashMaps.get("key4"));
	}

	@SuppressWarnings({ "unchecked", "static-access" })
	@Test
	public void saveFlashMap() throws InterruptedException {
		FlashMap flashMap = new FlashMap("key", "_flashKey");
		flashMap.put("name", "value");
		request.setAttribute(DefaultFlashMapManager.CURRENT_FLASH_MAP_ATTRIBUTE, flashMap);

		this.flashMapManager.setFlashMapTimeout(0);
		this.flashMapManager.requestCompleted(this.request);

		Thread.currentThread().sleep(1);
		
		String sessionKey = DefaultFlashMapManager.FLASH_MAPS_SESSION_ATTRIBUTE;
		Map<String, FlashMap> allFlashMaps = (Map<String, FlashMap>) this.request.getSession().getAttribute(sessionKey);
		
		assertSame(flashMap, allFlashMaps.get("key"));
		assertTrue(flashMap.isExpired());
	}

	@Test
	public void saveEmptyFlashMap() throws InterruptedException {
		FlashMap flashMap = new FlashMap("key", "_flashKey");
		request.setAttribute(DefaultFlashMapManager.CURRENT_FLASH_MAP_ATTRIBUTE, flashMap);

		this.flashMapManager.setFlashMapTimeout(0);
		this.flashMapManager.requestCompleted(this.request);

		assertNull(this.request.getSession().getAttribute(DefaultFlashMapManager.FLASH_MAPS_SESSION_ATTRIBUTE));
	}

}
