/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.web.servlet.support;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.util.WebUtils;

import static org.junit.Assert.*;

/**
 * Test fixture for testing {@link AbstractFlashMapManager} methods.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
public class FlashMapManagerTests {

	private final TestFlashMapManager flashMapManager = new TestFlashMapManager();

	private final MockHttpServletRequest request = new MockHttpServletRequest();

	private final MockHttpServletResponse response = new MockHttpServletResponse();


	@Test
	public void retrieveAndUpdateMatchByPath() {
		FlashMap flashMap = new FlashMap();
		flashMap.put("key", "value");
		flashMap.setTargetRequestPath("/path");

		this.flashMapManager.setFlashMaps(Arrays.asList(flashMap));

		this.request.setRequestURI("/path");
		FlashMap inputFlashMap = this.flashMapManager.retrieveAndUpdate(this.request, this.response);

		assertEquals(flashMap, inputFlashMap);
	}

	@Test // SPR-8779
	public void retrieveAndUpdateMatchByOriginatingPath() {
		FlashMap flashMap = new FlashMap();
		flashMap.put("key", "value");
		flashMap.setTargetRequestPath("/accounts");

		this.flashMapManager.setFlashMaps(Arrays.asList(flashMap));

		this.request.setAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE, "/accounts");
		this.request.setRequestURI("/mvc/accounts");
		FlashMap inputFlashMap = this.flashMapManager.retrieveAndUpdate(this.request, this.response);

		assertEquals(flashMap, inputFlashMap);
		assertEquals("Input FlashMap should have been removed", 0, this.flashMapManager.getFlashMaps().size());
	}

	@Test
	public void retrieveAndUpdateMatchWithTrailingSlash() {
		FlashMap flashMap = new FlashMap();
		flashMap.put("key", "value");
		flashMap.setTargetRequestPath("/path");

		this.flashMapManager.setFlashMaps(Arrays.asList(flashMap));

		this.request.setRequestURI("/path/");
		FlashMap inputFlashMap = this.flashMapManager.retrieveAndUpdate(this.request, this.response);

		assertEquals(flashMap, inputFlashMap);
		assertEquals("Input FlashMap should have been removed", 0, this.flashMapManager.getFlashMaps().size());
	}

	@Test
	public void retrieveAndUpdateMatchByParams() {
		FlashMap flashMap = new FlashMap();
		flashMap.put("key", "value");
		flashMap.addTargetRequestParam("number", "one");

		this.flashMapManager.setFlashMaps(Arrays.asList(flashMap));

		this.request.setQueryString("number=");
		FlashMap inputFlashMap = this.flashMapManager.retrieveAndUpdate(this.request, this.response);

		assertNull(inputFlashMap);
		assertEquals("FlashMap should not have been removed", 1, this.flashMapManager.getFlashMaps().size());

		this.request.setQueryString("number=two");
		inputFlashMap = this.flashMapManager.retrieveAndUpdate(this.request, this.response);

		assertNull(inputFlashMap);
		assertEquals("FlashMap should not have been removed", 1, this.flashMapManager.getFlashMaps().size());

		this.request.setQueryString("number=one");
		inputFlashMap = this.flashMapManager.retrieveAndUpdate(this.request, this.response);

		assertEquals(flashMap, inputFlashMap);
		assertEquals("Input FlashMap should have been removed", 0, this.flashMapManager.getFlashMaps().size());
	}

	@Test // SPR-8798
	public void retrieveAndUpdateMatchWithMultiValueParam() {
		FlashMap flashMap = new FlashMap();
		flashMap.put("name", "value");
		flashMap.addTargetRequestParam("id", "1");
		flashMap.addTargetRequestParam("id", "2");

		this.flashMapManager.setFlashMaps(Arrays.asList(flashMap));

		this.request.setQueryString("id=1");
		FlashMap inputFlashMap = this.flashMapManager.retrieveAndUpdate(this.request, this.response);

		assertNull(inputFlashMap);
		assertEquals("FlashMap should not have been removed", 1, this.flashMapManager.getFlashMaps().size());

		this.request.setQueryString("id=1&id=2");
		inputFlashMap = this.flashMapManager.retrieveAndUpdate(this.request, this.response);

		assertEquals(flashMap, inputFlashMap);
		assertEquals("Input FlashMap should have been removed", 0, this.flashMapManager.getFlashMaps().size());
	}

	@Test
	public void retrieveAndUpdateSortMultipleMatches() {
		FlashMap emptyFlashMap = new FlashMap();

		FlashMap flashMapOne = new FlashMap();
		flashMapOne.put("key1", "value1");
		flashMapOne.setTargetRequestPath("/one");

		FlashMap flashMapTwo = new FlashMap();
		flashMapTwo.put("key1", "value1");
		flashMapTwo.put("key2", "value2");
		flashMapTwo.setTargetRequestPath("/one/two");

		this.flashMapManager.setFlashMaps(Arrays.asList(emptyFlashMap, flashMapOne, flashMapTwo));

		this.request.setRequestURI("/one/two");
		FlashMap inputFlashMap = this.flashMapManager.retrieveAndUpdate(this.request, this.response);

		assertEquals(flashMapTwo, inputFlashMap);
		assertEquals("Input FlashMap should have been removed", 2, this.flashMapManager.getFlashMaps().size());
	}

	@Test
	public void retrieveAndUpdateRemoveExpired() {
		List<FlashMap> flashMaps = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			FlashMap expiredFlashMap = new FlashMap();
			expiredFlashMap.startExpirationPeriod(-1);
			flashMaps.add(expiredFlashMap);
		}
		this.flashMapManager.setFlashMaps(flashMaps);
		this.flashMapManager.retrieveAndUpdate(this.request, this.response);

		assertEquals("Expired instances should be removed even if the saved FlashMap is empty",
				0, this.flashMapManager.getFlashMaps().size());
	}

	@Test
	public void saveOutputFlashMapEmpty() {
		FlashMap flashMap = new FlashMap();

		this.flashMapManager.saveOutputFlashMap(flashMap, this.request, this.response);
		List<FlashMap> allMaps = this.flashMapManager.getFlashMaps();

		assertNull(allMaps);
	}

	@Test
	public void saveOutputFlashMap() {
		FlashMap flashMap = new FlashMap();
		flashMap.put("name", "value");

		this.flashMapManager.setFlashMapTimeout(-1); // expire immediately so we can check expiration started
		this.flashMapManager.saveOutputFlashMap(flashMap, this.request, this.response);
		List<FlashMap> allMaps = this.flashMapManager.getFlashMaps();

		assertNotNull(allMaps);
		assertSame(flashMap, allMaps.get(0));
		assertTrue(flashMap.isExpired());
	}

	@Test
	public void saveOutputFlashMapDecodeTargetPath() {
		FlashMap flashMap = new FlashMap();
		flashMap.put("key", "value");

		flashMap.setTargetRequestPath("/once%20upon%20a%20time");
		this.flashMapManager.saveOutputFlashMap(flashMap, this.request, this.response);

		assertEquals("/once upon a time", flashMap.getTargetRequestPath());
	}

	@Test
	public void saveOutputFlashMapNormalizeTargetPath() {
		FlashMap flashMap = new FlashMap();
		flashMap.put("key", "value");

		flashMap.setTargetRequestPath(".");
		this.request.setRequestURI("/once/upon/a/time");
		this.flashMapManager.saveOutputFlashMap(flashMap, this.request, this.response);

		assertEquals("/once/upon/a", flashMap.getTargetRequestPath());

		flashMap.setTargetRequestPath("./");
		this.request.setRequestURI("/once/upon/a/time");
		this.flashMapManager.saveOutputFlashMap(flashMap, this.request, this.response);

		assertEquals("/once/upon/a/", flashMap.getTargetRequestPath());

		flashMap.setTargetRequestPath("..");
		this.request.setRequestURI("/once/upon/a/time");
		this.flashMapManager.saveOutputFlashMap(flashMap, this.request, this.response);

		assertEquals("/once/upon", flashMap.getTargetRequestPath());

		flashMap.setTargetRequestPath("../");
		this.request.setRequestURI("/once/upon/a/time");
		this.flashMapManager.saveOutputFlashMap(flashMap, this.request, this.response);

		assertEquals("/once/upon/", flashMap.getTargetRequestPath());

		flashMap.setTargetRequestPath("../../only");
		this.request.setRequestURI("/once/upon/a/time");
		this.flashMapManager.saveOutputFlashMap(flashMap, this.request, this.response);

		assertEquals("/once/only", flashMap.getTargetRequestPath());
	}

	@Test // gh-23240
	public void saveOutputFlashMapAndNormalizeEmptyTargetPath() {
		FlashMap flashMap = new FlashMap();
		flashMap.put("key", "value");

		flashMap.setTargetRequestPath("");
		this.flashMapManager.saveOutputFlashMap(flashMap, this.request, this.response);

		assertEquals("", flashMap.getTargetRequestPath());
	}

	@Test // SPR-9657, SPR-11504
	public void saveOutputFlashMapDecodeParameters() throws Exception {
		FlashMap flashMap = new FlashMap();
		flashMap.put("key", "value");
		flashMap.setTargetRequestPath("/path");
		flashMap.addTargetRequestParam("param", "%D0%90%D0%90");
		flashMap.addTargetRequestParam("param", "%D0%91%D0%91");
		flashMap.addTargetRequestParam("param", "%D0%92%D0%92");
		flashMap.addTargetRequestParam("%3A%2F%3F%23%5B%5D%40", "value");

		this.request.setCharacterEncoding("UTF-8");
		this.flashMapManager.saveOutputFlashMap(flashMap, this.request, this.response);

		MockHttpServletRequest requestAfterRedirect = new MockHttpServletRequest("GET", "/path");
		requestAfterRedirect.setQueryString(
				"param=%D0%90%D0%90&param=%D0%91%D0%91&param=%D0%92%D0%92&%3A%2F%3F%23%5B%5D%40=value");
		requestAfterRedirect.addParameter("param", "\u0410\u0410");
		requestAfterRedirect.addParameter("param", "\u0411\u0411");
		requestAfterRedirect.addParameter("param", "\u0412\u0412");
		requestAfterRedirect.addParameter(":/?#[]@", "value");

		flashMap = this.flashMapManager.retrieveAndUpdate(requestAfterRedirect, new MockHttpServletResponse());
		assertNotNull(flashMap);
		assertEquals(1, flashMap.size());
		assertEquals("value", flashMap.get("key"));
	}

	@Test // SPR-12569
	public void flashAttributesWithQueryParamsWithSpace() throws Exception {
		String encodedValue = URLEncoder.encode("1 2", "UTF-8");

		FlashMap flashMap = new FlashMap();
		flashMap.put("key", "value");
		flashMap.setTargetRequestPath("/path");
		flashMap.addTargetRequestParam("param", encodedValue);

		this.request.setCharacterEncoding("UTF-8");
		this.flashMapManager.saveOutputFlashMap(flashMap, this.request, this.response);

		MockHttpServletRequest requestAfterRedirect = new MockHttpServletRequest("GET", "/path");
		requestAfterRedirect.setQueryString("param=" + encodedValue);
		requestAfterRedirect.addParameter("param", "1 2");

		flashMap = this.flashMapManager.retrieveAndUpdate(requestAfterRedirect, new MockHttpServletResponse());
		assertNotNull(flashMap);
		assertEquals(1, flashMap.size());
		assertEquals("value", flashMap.get("key"));
	}

	@Test // SPR-15505
	public void retrieveAndUpdateMatchByOriginatingPathAndQueryString() {
		FlashMap flashMap = new FlashMap();
		flashMap.put("key", "value");
		flashMap.setTargetRequestPath("/accounts");
		flashMap.addTargetRequestParam("a", "b");

		this.flashMapManager.setFlashMaps(Collections.singletonList(flashMap));

		this.request.setAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE, "/accounts");
		this.request.setAttribute(WebUtils.FORWARD_QUERY_STRING_ATTRIBUTE, "a=b");
		this.request.setRequestURI("/mvc/accounts");
		this.request.setQueryString("x=y");
		FlashMap inputFlashMap = this.flashMapManager.retrieveAndUpdate(this.request, this.response);

		assertEquals(flashMap, inputFlashMap);
		assertEquals("Input FlashMap should have been removed", 0, this.flashMapManager.getFlashMaps().size());
	}


	private static class TestFlashMapManager extends AbstractFlashMapManager {

		private List<FlashMap> flashMaps;


		public void setFlashMaps(List<FlashMap> flashMaps) {
			this.flashMaps = new CopyOnWriteArrayList<>(flashMaps);
		}

		public List<FlashMap> getFlashMaps() {
			return this.flashMaps;
		}


		@Override
		protected List<FlashMap> retrieveFlashMaps(HttpServletRequest request) {
			return this.flashMaps;
		}

		@Override
		protected void updateFlashMaps(List<FlashMap> maps, HttpServletRequest request, HttpServletResponse response) {
			this.flashMaps = maps;
		}
	}

}
