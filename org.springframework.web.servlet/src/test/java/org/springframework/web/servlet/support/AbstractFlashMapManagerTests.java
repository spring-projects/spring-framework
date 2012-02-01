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

package org.springframework.web.servlet.support;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.util.WebUtils;

/**
 * Test fixture for testing {@link AbstractFlashMapManager} methods.
 *
 * @author Rossen Stoyanchev
 */
public class AbstractFlashMapManagerTests {

	private TestFlashMapManager flashMapManager;

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;

	@Before
	public void setup() {
		this.flashMapManager = new TestFlashMapManager();
		this.request = new MockHttpServletRequest();
		this.response = new MockHttpServletResponse();
	}

	@Test
	public void getFlashMapForRequestByPath() {
		FlashMap flashMap = new FlashMap();
		flashMap.put("key", "value");
		flashMap.setTargetRequestPath("/path");

		this.flashMapManager.setFlashMaps(flashMap);

		this.request.setRequestURI("/path");
		Map<String, ?> inputFlashMap = this.flashMapManager.getFlashMapForRequest(this.request);

		assertEquals(flashMap, inputFlashMap);
		assertEquals("Input FlashMap should have been removed", 0, this.flashMapManager.getFlashMaps().size());
	}

	// SPR-8779

	@Test
	public void getFlashMapForRequestByOriginatingPath() {
		FlashMap flashMap = new FlashMap();
		flashMap.put("key", "value");
		flashMap.setTargetRequestPath("/accounts");

		this.flashMapManager.setFlashMaps(flashMap);

		this.request.setAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE, "/accounts");
		this.request.setRequestURI("/mvc/accounts");
		Map<String, ?> inputFlashMap = this.flashMapManager.getFlashMapForRequest(this.request);

		assertEquals(flashMap, inputFlashMap);
		assertEquals("Input FlashMap should have been removed", 0, this.flashMapManager.getFlashMaps().size());
	}

	@Test
	public void getFlashMapForRequestByPathWithTrailingSlash() {
		FlashMap flashMap = new FlashMap();
		flashMap.put("key", "value");
		flashMap.setTargetRequestPath("/path");

		this.flashMapManager.setFlashMaps(flashMap);

		this.request.setRequestURI("/path/");
		Map<String, ?> inputFlashMap = this.flashMapManager.getFlashMapForRequest(this.request);

		assertEquals(flashMap, inputFlashMap);
		assertEquals("Input FlashMap should have been removed", 0, this.flashMapManager.getFlashMaps().size());
	}

	@Test
	public void getFlashMapForRequestWithParams() {
		FlashMap flashMap = new FlashMap();
		flashMap.put("key", "value");
		flashMap.addTargetRequestParam("number", "one");

		this.flashMapManager.setFlashMaps(flashMap);

		this.request.setParameter("number", (String) null);
		Map<String, ?> inputFlashMap = this.flashMapManager.getFlashMapForRequest(this.request);

		assertNull(inputFlashMap);
		assertEquals("FlashMap should not have been removed", 1, this.flashMapManager.getFlashMaps().size());

		this.request.setParameter("number", "two");
		inputFlashMap = this.flashMapManager.getFlashMapForRequest(this.request);

		assertNull(inputFlashMap);
		assertEquals("FlashMap should not have been removed", 1, this.flashMapManager.getFlashMaps().size());

		this.request.setParameter("number", "one");
		inputFlashMap = this.flashMapManager.getFlashMapForRequest(this.request);

		assertEquals(flashMap, inputFlashMap);
		assertEquals("Input FlashMap should have been removed", 0, this.flashMapManager.getFlashMaps().size());
	}

	// SPR-8798

	@Test
	public void getFlashMapForRequestWithMultiValueParam() {
		FlashMap flashMap = new FlashMap();
		flashMap.put("name", "value");
		flashMap.addTargetRequestParam("id", "1");
		flashMap.addTargetRequestParam("id", "2");

		this.flashMapManager.setFlashMaps(flashMap);

		this.request.setParameter("id", "1");
		Map<String, ?> inputFlashMap = this.flashMapManager.getFlashMapForRequest(this.request);

		assertNull(inputFlashMap);
		assertEquals("FlashMap should not have been removed", 1, this.flashMapManager.getFlashMaps().size());

		this.request.addParameter("id", "2");
		inputFlashMap = this.flashMapManager.getFlashMapForRequest(this.request);

		assertEquals(flashMap, inputFlashMap);
		assertEquals("Input FlashMap should have been removed", 0, this.flashMapManager.getFlashMaps().size());
	}

	@Test
	public void getFlashMapForRequestSortOrder() {
		FlashMap emptyFlashMap = new FlashMap();

		FlashMap flashMapOne = new FlashMap();
		flashMapOne.put("key1", "value1");
		flashMapOne.setTargetRequestPath("/one");

		FlashMap flashMapTwo = new FlashMap();
		flashMapTwo.put("key1", "value1");
		flashMapTwo.put("key2", "value2");
		flashMapTwo.setTargetRequestPath("/one/two");

		this.flashMapManager.setFlashMaps(emptyFlashMap, flashMapOne, flashMapTwo);

		this.request.setRequestURI("/one/two");
		Map<String, ?> inputFlashMap = this.flashMapManager.getFlashMapForRequest(this.request);

		assertEquals(flashMapTwo, inputFlashMap);
	}

	@Test
	public void saveFlashMapEmpty() throws InterruptedException {
		FlashMap flashMap = new FlashMap();

		this.flashMapManager.save(flashMap, this.request, this.response);
		List<FlashMap> allMaps = this.flashMapManager.getFlashMaps();

		assertNull(allMaps);
	}

	@Test
	public void saveFlashMap() throws InterruptedException {
		FlashMap flashMap = new FlashMap();
		flashMap.put("name", "value");

		this.flashMapManager.setFlashMapTimeout(-1); // expire immediately so we can check expiration started
		this.flashMapManager.save(flashMap, this.request, this.response);
		List<FlashMap> allMaps = this.flashMapManager.getFlashMaps();

		assertNotNull(allMaps);
		assertSame(flashMap, allMaps.get(0));
		assertTrue(flashMap.isExpired());
	}

	@Test
	public void saveFlashMapDecodeTargetPath() throws InterruptedException {
		FlashMap flashMap = new FlashMap();
		flashMap.put("key", "value");

		flashMap.setTargetRequestPath("/once%20upon%20a%20time");
		this.flashMapManager.save(flashMap, this.request, this.response);

		assertEquals("/once upon a time", flashMap.getTargetRequestPath());
	}

	@Test
	public void saveFlashMapNormalizeTargetPath() throws InterruptedException {
		FlashMap flashMap = new FlashMap();
		flashMap.put("key", "value");

		flashMap.setTargetRequestPath(".");
		this.request.setRequestURI("/once/upon/a/time");
		this.flashMapManager.save(flashMap, this.request, this.response);

		assertEquals("/once/upon/a", flashMap.getTargetRequestPath());

		flashMap.setTargetRequestPath("./");
		this.request.setRequestURI("/once/upon/a/time");
		this.flashMapManager.save(flashMap, this.request, this.response);

		assertEquals("/once/upon/a/", flashMap.getTargetRequestPath());

		flashMap.setTargetRequestPath("..");
		this.request.setRequestURI("/once/upon/a/time");
		this.flashMapManager.save(flashMap, this.request, this.response);

		assertEquals("/once/upon", flashMap.getTargetRequestPath());

		flashMap.setTargetRequestPath("../");
		this.request.setRequestURI("/once/upon/a/time");
		this.flashMapManager.save(flashMap, this.request, this.response);

		assertEquals("/once/upon/", flashMap.getTargetRequestPath());

		flashMap.setTargetRequestPath("../../only");
		this.request.setRequestURI("/once/upon/a/time");
		this.flashMapManager.save(flashMap, this.request, this.response);

		assertEquals("/once/only", flashMap.getTargetRequestPath());
	}

	@Test
	public void saveFlashMapAndRemoveExpired() throws InterruptedException {
		List<FlashMap> flashMaps = new ArrayList<FlashMap>();
		for (int i=0; i < 5; i++) {
			FlashMap flashMap = new FlashMap();
			flashMap.startExpirationPeriod(-1);
			flashMaps.add(flashMap);
		}
		this.flashMapManager.setFlashMaps(flashMaps);
		this.flashMapManager.save(new FlashMap(), request, response);

		assertEquals("Expired instances should be removed even if the saved FlashMap is empty", 
				0, this.flashMapManager.getFlashMaps().size());
	}


	private static class TestFlashMapManager extends AbstractFlashMapManager {

		private List<FlashMap> flashMaps;

		public List<FlashMap> getFlashMaps() {
			return this.flashMaps;
		}

		public void setFlashMaps(FlashMap... flashMaps) {
			setFlashMaps(Arrays.asList(flashMaps));
		}

		public void setFlashMaps(List<FlashMap> flashMaps) {
			this.flashMaps = new CopyOnWriteArrayList<FlashMap>(flashMaps);
		}

		@Override
		protected List<FlashMap> retrieveFlashMaps(HttpServletRequest request) {
			return this.flashMaps;
		}

		@Override
		protected void updateFlashMaps(List<FlashMap> flashMaps, HttpServletRequest request,
				HttpServletResponse response) {
			this.flashMaps = flashMaps;
		}
	}

}
