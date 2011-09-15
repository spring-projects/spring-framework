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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.springframework.web.servlet.FlashMapManager.INPUT_FLASH_MAP_ATTRIBUTE;
import static org.springframework.web.servlet.FlashMapManager.OUTPUT_FLASH_MAP_ATTRIBUTE;

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
		this.flashMapManager.requestStarted(this.request);
		FlashMap flashMap = RequestContextUtils.getOutputFlashMap(request);
		
		assertNotNull("Current FlashMap not found", flashMap);
	}

	@Test
	public void requestStartedAlready() {
		FlashMap flashMap = new FlashMap();
		this.request.setAttribute(OUTPUT_FLASH_MAP_ATTRIBUTE, flashMap);
		this.flashMapManager.requestStarted(this.request);
		
		assertSame(flashMap, RequestContextUtils.getOutputFlashMap(request));
	}
	
	@Test
	public void lookupFlashMapByPath() {
		FlashMap flashMap = new FlashMap();
		flashMap.put("key", "value");
		flashMap.setTargetRequestPath("/path");
		
		List<FlashMap> allMaps = createFlashMaps();
		allMaps.add(flashMap);
	
		this.request.setRequestURI("/path");
		this.flashMapManager.requestStarted(this.request);
		
		assertEquals(flashMap, RequestContextUtils.getInputFlashMap(this.request));
		assertEquals("Input FlashMap should have been removed", 0, getFlashMaps().size());		
	}
	
	@Test
	public void lookupFlashMapByPathWithTrailingSlash() {
		FlashMap flashMap = new FlashMap();
		flashMap.put("key", "value");
		flashMap.setTargetRequestPath("/path");
		
		List<FlashMap> allMaps = createFlashMaps();
		allMaps.add(flashMap);
	
		this.request.setRequestURI("/path/");
		this.flashMapManager.requestStarted(this.request);
		
		assertEquals(flashMap, RequestContextUtils.getInputFlashMap(this.request));
		assertEquals("Input FlashMap should have been removed", 0, getFlashMaps().size());		
	}

	@Test
	public void lookupFlashMapWithParams() {
		FlashMap flashMap = new FlashMap();
		flashMap.put("key", "value");
		flashMap.addTargetRequestParam("number", "one");
		
		List<FlashMap> allMaps = createFlashMaps();
		allMaps.add(flashMap);
	
		this.request.setParameter("number", (String) null);
		this.flashMapManager.requestStarted(this.request);

		assertNull(RequestContextUtils.getInputFlashMap(this.request));
		assertEquals("FlashMap should have been removed", 1, getFlashMaps().size());		

		clearRequestAttributes();
		this.request.setParameter("number", "two");
		this.flashMapManager.requestStarted(this.request);

		assertNull(RequestContextUtils.getInputFlashMap(this.request));
		assertEquals("FlashMap should have been removed", 1, getFlashMaps().size());		

		clearRequestAttributes();
		this.request.setParameter("number", "one");
		this.flashMapManager.requestStarted(this.request);

		assertEquals(flashMap, RequestContextUtils.getInputFlashMap(this.request));
		assertEquals("Input FlashMap should have been removed", 0, getFlashMaps().size());		
	}
	
	@Test
	public void lookupFlashMapSortOrder() {
		FlashMap emptyFlashMap = new FlashMap();

		FlashMap flashMapOne = new FlashMap();
		flashMapOne.put("key1", "value1");
		flashMapOne.setTargetRequestPath("/one");

		FlashMap flashMapTwo = new FlashMap();
		flashMapTwo.put("key1", "value1");
		flashMapTwo.put("key2", "value2");
		flashMapTwo.setTargetRequestPath("/one/two");
		
		List<FlashMap> allMaps = createFlashMaps();
		allMaps.add(emptyFlashMap);
		allMaps.add(flashMapOne);
		allMaps.add(flashMapTwo);
		Collections.shuffle(allMaps);

		this.request.setRequestURI("/one/two");
		this.flashMapManager.requestStarted(this.request);
		
		assertEquals(flashMapTwo, request.getAttribute(INPUT_FLASH_MAP_ATTRIBUTE));
	}

	@Test
	public void removeExpiredFlashMaps() throws InterruptedException {
		List<FlashMap> allMaps = createFlashMaps();
		for (int i=0; i < 5; i++) {
			FlashMap flashMap = new FlashMap();
			allMaps.add(flashMap);
			flashMap.startExpirationPeriod(0);
		}
		Thread.sleep(100);
		this.flashMapManager.requestStarted(this.request);
		
		assertEquals(0, allMaps.size());
	}

	@Test
	public void saveFlashMapWithoutAttributes() throws InterruptedException {
		this.flashMapManager.requestStarted(this.request);
		this.flashMapManager.requestCompleted(this.request);

		assertNull(getFlashMaps());
	}

	@Test
	public void saveFlashMapNotCreatedByThisManager() throws InterruptedException {
		FlashMap flashMap = new FlashMap();
		this.request.setAttribute(OUTPUT_FLASH_MAP_ATTRIBUTE, flashMap);
		this.flashMapManager.requestCompleted(this.request);

		assertNull(getFlashMaps());
	}
	
	@Test
	public void saveFlashMapWithAttributes() throws InterruptedException {
		this.flashMapManager.requestStarted(this.request);
		FlashMap flashMap = RequestContextUtils.getOutputFlashMap(this.request);
		flashMap.put("name", "value");

		this.flashMapManager.setFlashMapTimeout(0);
		this.flashMapManager.requestCompleted(this.request);

		Thread.sleep(100);
		
		List<FlashMap> allMaps = getFlashMaps();
		
		assertNotNull(allMaps);
		assertSame(flashMap, allMaps.get(0));
		assertTrue(flashMap.isExpired());
	}

	@Test
	public void decodeTargetPath() throws InterruptedException {
		this.flashMapManager.requestStarted(this.request);
		FlashMap flashMap = RequestContextUtils.getOutputFlashMap(this.request);
		flashMap.put("key", "value");

		flashMap.setTargetRequestPath("/once%20upon%20a%20time");
		this.flashMapManager.requestCompleted(this.request);
		
		assertEquals("/once upon a time", flashMap.getTargetRequestPath());
	}

	@Test
	public void normalizeTargetPath() throws InterruptedException {
		this.flashMapManager.requestStarted(this.request);
		FlashMap flashMap = RequestContextUtils.getOutputFlashMap(this.request);
		flashMap.put("key", "value");

		flashMap.setTargetRequestPath(".");
		this.request.setRequestURI("/once/upon/a/time");
		this.flashMapManager.requestCompleted(this.request);

		assertEquals("/once/upon/a", flashMap.getTargetRequestPath());

		flashMap.setTargetRequestPath("./");
		this.request.setRequestURI("/once/upon/a/time");
		this.flashMapManager.requestCompleted(this.request);

		assertEquals("/once/upon/a/", flashMap.getTargetRequestPath());

		flashMap.setTargetRequestPath("..");
		this.request.setRequestURI("/once/upon/a/time");
		this.flashMapManager.requestCompleted(this.request);

		assertEquals("/once/upon", flashMap.getTargetRequestPath());

		flashMap.setTargetRequestPath("../");
		this.request.setRequestURI("/once/upon/a/time");
		this.flashMapManager.requestCompleted(this.request);

		assertEquals("/once/upon/", flashMap.getTargetRequestPath());

		flashMap.setTargetRequestPath("../../only");
		this.request.setRequestURI("/once/upon/a/time");
		this.flashMapManager.requestCompleted(this.request);

		assertEquals("/once/only", flashMap.getTargetRequestPath());
	}

	@SuppressWarnings("unchecked")
	private List<FlashMap> getFlashMaps() {
		return (List<FlashMap>) this.request.getSession().getAttribute(DefaultFlashMapManager.class + ".FLASH_MAPS");
	}

	private List<FlashMap> createFlashMaps() {
		List<FlashMap> allMaps = new CopyOnWriteArrayList<FlashMap>();
		this.request.getSession().setAttribute(DefaultFlashMapManager.class + ".FLASH_MAPS", allMaps);
		return allMaps;
	}
	
	private void clearRequestAttributes() {
		request.removeAttribute(INPUT_FLASH_MAP_ATTRIBUTE);
		request.removeAttribute(OUTPUT_FLASH_MAP_ATTRIBUTE);
	}

}
