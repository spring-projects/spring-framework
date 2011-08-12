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

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.FlashMap;

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
	public void requestStarted() {
		boolean initialized = this.flashMapManager.requestStarted(this.request);

		assertTrue("Current FlashMap not initialized on first call", initialized);
		assertNotNull("Current FlashMap not found", RequestContextUtils.getFlashMap(request));
		
		initialized = this.flashMapManager.requestStarted(this.request);
		
		assertFalse("Current FlashMap initialized twice", initialized);
	}

	@Test
	public void lookupPreviousFlashMap() {
		FlashMap flashMap = new FlashMap();

		List<FlashMap> allMaps = createFlashMapsSessionAttribute();
		allMaps.add(flashMap);

		this.flashMapManager.requestStarted(this.request);
		
		assertSame(flashMap, request.getAttribute(DefaultFlashMapManager.PREVIOUS_FLASH_MAP_ATTRIBUTE));
		assertEquals("Previous FlashMap should have been removed", 0, allMaps.size());
	}
	
	@Test
	public void lookupPreviousFlashMapExpectedUrlPath() {
		FlashMap emptyFlashMap = new FlashMap();

		FlashMap oneFlashMap = new FlashMap();
		oneFlashMap.setExpectedUrl(null, "/one");

		FlashMap oneOtherFlashMap = new FlashMap();
		oneOtherFlashMap.setExpectedUrl(null, "/one/other");
		
		List<FlashMap> allMaps = createFlashMapsSessionAttribute();
		allMaps.add(emptyFlashMap);
		allMaps.add(oneFlashMap);
		allMaps.add(oneOtherFlashMap);
		Collections.shuffle(allMaps);

		this.request.setRequestURI("/one");
		this.flashMapManager.requestStarted(this.request);
		
		assertSame(oneFlashMap, request.getAttribute(DefaultFlashMapManager.PREVIOUS_FLASH_MAP_ATTRIBUTE));
	}

	@Test
	public void removeExpiredFlashMaps() throws InterruptedException {
		List<FlashMap> allMaps = createFlashMapsSessionAttribute();
		for (int i=0; i < 5; i++) {
			FlashMap flashMap = new FlashMap();
			allMaps.add(flashMap);
			flashMap.startExpirationPeriod(0);
		}
		
		Thread.sleep(5);

		this.flashMapManager.requestStarted(this.request);
		
		assertEquals(0, allMaps.size());
	}

	@Test
	public void saveFlashMap() throws InterruptedException {
		FlashMap flashMap = new FlashMap();
		flashMap.put("name", "value");
		request.setAttribute(DefaultFlashMapManager.CURRENT_FLASH_MAP_ATTRIBUTE, flashMap);

		this.flashMapManager.setFlashMapTimeout(0);
		this.flashMapManager.requestCompleted(this.request);

		Thread.sleep(1);
		
		List<FlashMap> allMaps = getFlashMapsSessionAttribute();
		
		assertNotNull(allMaps);
		assertSame(flashMap, allMaps.get(0));
		assertTrue(flashMap.isExpired());
	}

	@Test
	public void saveFlashMapIsEmpty() throws InterruptedException {
		request.setAttribute(DefaultFlashMapManager.CURRENT_FLASH_MAP_ATTRIBUTE, new FlashMap());
		this.flashMapManager.requestCompleted(this.request);

		assertNull(getFlashMapsSessionAttribute());
	}

	@SuppressWarnings("unchecked")
	private List<FlashMap> getFlashMapsSessionAttribute() {
		return (List<FlashMap>) this.request.getSession().getAttribute(DefaultFlashMapManager.class + ".FLASH_MAPS");
	}

	private List<FlashMap> createFlashMapsSessionAttribute() {
		List<FlashMap> allMaps = new CopyOnWriteArrayList<FlashMap>();
		this.request.getSession().setAttribute(DefaultFlashMapManager.class + ".FLASH_MAPS", allMaps);
		return allMaps;
	}

}
