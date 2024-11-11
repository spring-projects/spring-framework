/*
 * Copyright 2002-2024 the original author or authors.
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.assertj.core.api.ObjectAssert;
import org.junit.jupiter.api.Test;

import org.springframework.lang.Nullable;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;
import org.springframework.web.util.WebUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test fixture for testing {@link AbstractFlashMapManager} methods.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
class FlashMapManagerTests {

	private final TestFlashMapManager flashMapManager = new TestFlashMapManager();

	private final MockHttpServletRequest request = new MockHttpServletRequest();

	private final MockHttpServletResponse response = new MockHttpServletResponse();


	@Test
	void retrieveAndUpdateMatchByPath() {
		FlashMap flashMap = new FlashMap();
		flashMap.put("key", "value");
		flashMap.setTargetRequestPath("/path");

		this.flashMapManager.setFlashMaps(List.of(flashMap));

		this.request.setRequestURI("/path");
		FlashMap inputFlashMap = this.flashMapManager.retrieveAndUpdate(this.request, this.response);

		assertThatFlashMap(inputFlashMap).isEqualTo(flashMap);
	}

	@Test // SPR-8779
	public void retrieveAndUpdateMatchByOriginatingPath() {
		FlashMap flashMap = new FlashMap();
		flashMap.put("key", "value");
		flashMap.setTargetRequestPath("/accounts");

		this.flashMapManager.setFlashMaps(List.of(flashMap));

		this.request.setAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE, "/accounts");
		this.request.setRequestURI("/mvc/accounts");
		FlashMap inputFlashMap = this.flashMapManager.retrieveAndUpdate(this.request, this.response);

		assertThatFlashMap(inputFlashMap).isEqualTo(flashMap);
		assertThat(this.flashMapManager.getFlashMaps()).as("Input FlashMap should have been removed").isEmpty();
	}

	@Test
	void retrieveAndUpdateMatchWithTrailingSlash() {
		FlashMap flashMap = new FlashMap();
		flashMap.put("key", "value");
		flashMap.setTargetRequestPath("/path");

		this.flashMapManager.setFlashMaps(List.of(flashMap));

		this.request.setRequestURI("/path/");
		FlashMap inputFlashMap = this.flashMapManager.retrieveAndUpdate(this.request, this.response);

		assertThatFlashMap(inputFlashMap).isEqualTo(flashMap);
		assertThat(this.flashMapManager.getFlashMaps()).as("Input FlashMap should have been removed").isEmpty();
	}

	@Test
	void retrieveAndUpdateMatchByParams() {
		FlashMap flashMap = new FlashMap();
		flashMap.put("key", "value");
		flashMap.addTargetRequestParam("number", "one");

		this.flashMapManager.setFlashMaps(List.of(flashMap));

		this.request.setQueryString("number=");
		FlashMap inputFlashMap = this.flashMapManager.retrieveAndUpdate(this.request, this.response);

		assertThatFlashMap(inputFlashMap).isNull();
		assertThat(this.flashMapManager.getFlashMaps()).as("FlashMap should not have been removed").hasSize(1);

		this.request.setQueryString("number=two");
		inputFlashMap = this.flashMapManager.retrieveAndUpdate(this.request, this.response);

		assertThatFlashMap(inputFlashMap).isNull();
		assertThat(this.flashMapManager.getFlashMaps()).as("FlashMap should not have been removed").hasSize(1);

		this.request.setQueryString("number=one");
		inputFlashMap = this.flashMapManager.retrieveAndUpdate(this.request, this.response);

		assertThatFlashMap(inputFlashMap).isEqualTo(flashMap);
		assertThat(this.flashMapManager.getFlashMaps()).as("Input FlashMap should have been removed").isEmpty();
	}

	@Test // SPR-8798
	public void retrieveAndUpdateMatchWithMultiValueParam() {
		FlashMap flashMap = new FlashMap();
		flashMap.put("name", "value");
		flashMap.addTargetRequestParam("id", "1");
		flashMap.addTargetRequestParam("id", "2");

		this.flashMapManager.setFlashMaps(List.of(flashMap));

		this.request.setQueryString("id=1");
		FlashMap inputFlashMap = this.flashMapManager.retrieveAndUpdate(this.request, this.response);

		assertThatFlashMap(inputFlashMap).isNull();
		assertThat(this.flashMapManager.getFlashMaps()).as("FlashMap should not have been removed").hasSize(1);

		this.request.setQueryString("id=1&id=2");
		inputFlashMap = this.flashMapManager.retrieveAndUpdate(this.request, this.response);

		assertThatFlashMap(inputFlashMap).isEqualTo(flashMap);
		assertThat(this.flashMapManager.getFlashMaps()).as("Input FlashMap should have been removed").isEmpty();
	}

	@Test
	void retrieveAndUpdateSortMultipleMatches() {
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

		assertThatFlashMap(inputFlashMap).isEqualTo(flashMapTwo);
		assertThat(this.flashMapManager.getFlashMaps()).as("Input FlashMap should have been removed").hasSize(2);
	}

	@Test
	void retrieveAndUpdateRemoveExpired() {
		List<FlashMap> flashMaps = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			FlashMap expiredFlashMap = new FlashMap();
			expiredFlashMap.startExpirationPeriod(-1);
			flashMaps.add(expiredFlashMap);
		}
		this.flashMapManager.setFlashMaps(flashMaps);
		this.flashMapManager.retrieveAndUpdate(this.request, this.response);

		assertThat(this.flashMapManager.getFlashMaps()).as("Expired instances should be removed even if the saved FlashMap is empty")
				.isEmpty();
	}

	@Test
	void saveOutputFlashMapEmpty() {
		FlashMap flashMap = new FlashMap();

		this.flashMapManager.saveOutputFlashMap(flashMap, this.request, this.response);
		List<FlashMap> allMaps = this.flashMapManager.getFlashMaps();

		assertThat(allMaps).isNull();
	}

	@Test
	void saveOutputFlashMap() {
		FlashMap flashMap = new FlashMap();
		flashMap.put("name", "value");

		this.flashMapManager.setFlashMapTimeout(-1); // expire immediately so we can check expiration started
		this.flashMapManager.saveOutputFlashMap(flashMap, this.request, this.response);
		List<FlashMap> allMaps = this.flashMapManager.getFlashMaps();

		assertThat(allMaps).isNotNull();
		assertThatFlashMap(allMaps.get(0)).isSameAs(flashMap);
		assertThat(flashMap.isExpired()).isTrue();
	}

	@Test
	void saveOutputFlashMapDecodeTargetPath() {
		FlashMap flashMap = new FlashMap();
		flashMap.put("key", "value");

		flashMap.setTargetRequestPath("/once%20upon%20a%20time");
		this.flashMapManager.saveOutputFlashMap(flashMap, this.request, this.response);

		assertThat(flashMap.getTargetRequestPath()).isEqualTo("/once upon a time");
	}

	@Test
	void saveOutputFlashMapNormalizeTargetPath() {
		FlashMap flashMap = new FlashMap();
		flashMap.put("key", "value");

		flashMap.setTargetRequestPath(".");
		this.request.setRequestURI("/once/upon/a/time");
		this.flashMapManager.saveOutputFlashMap(flashMap, this.request, this.response);

		assertThat(flashMap.getTargetRequestPath()).isEqualTo("/once/upon/a");

		flashMap.setTargetRequestPath("./");
		this.request.setRequestURI("/once/upon/a/time");
		this.flashMapManager.saveOutputFlashMap(flashMap, this.request, this.response);

		assertThat(flashMap.getTargetRequestPath()).isEqualTo("/once/upon/a/");

		flashMap.setTargetRequestPath("..");
		this.request.setRequestURI("/once/upon/a/time");
		this.flashMapManager.saveOutputFlashMap(flashMap, this.request, this.response);

		assertThat(flashMap.getTargetRequestPath()).isEqualTo("/once/upon");

		flashMap.setTargetRequestPath("../");
		this.request.setRequestURI("/once/upon/a/time");
		this.flashMapManager.saveOutputFlashMap(flashMap, this.request, this.response);

		assertThat(flashMap.getTargetRequestPath()).isEqualTo("/once/upon/");

		flashMap.setTargetRequestPath("../../only");
		this.request.setRequestURI("/once/upon/a/time");
		this.flashMapManager.saveOutputFlashMap(flashMap, this.request, this.response);

		assertThat(flashMap.getTargetRequestPath()).isEqualTo("/once/only");
	}

	@Test // gh-23240
	public void saveOutputFlashMapAndNormalizeEmptyTargetPath() {
		FlashMap flashMap = new FlashMap();
		flashMap.put("key", "value");

		flashMap.setTargetRequestPath("");
		this.flashMapManager.saveOutputFlashMap(flashMap, this.request, this.response);

		assertThat(flashMap.getTargetRequestPath()).isEmpty();
	}

	@Test // SPR-9657, SPR-11504
	public void saveOutputFlashMapDecodeParameters() {
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
		assertThatFlashMap(flashMap).isNotNull();
		assertThat(flashMap.size()).isEqualTo(1);
		assertThat(flashMap.get("key")).isEqualTo("value");
	}

	@Test // SPR-12569
	public void flashAttributesWithQueryParamsWithSpace() {
		String encodedValue = URLEncoder.encode("1 2", StandardCharsets.UTF_8);

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
		assertThatFlashMap(flashMap).isNotNull();
		assertThat(flashMap.size()).isEqualTo(1);
		assertThat(flashMap.get("key")).isEqualTo("value");
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

		assertThatFlashMap(inputFlashMap).isEqualTo(flashMap);
		assertThat(this.flashMapManager.getFlashMaps()).as("Input FlashMap should have been removed").isEmpty();
	}


	private static ObjectAssert<Object> assertThatFlashMap(@Nullable FlashMap flashMap) {
		return assertThat((Object) flashMap);
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
