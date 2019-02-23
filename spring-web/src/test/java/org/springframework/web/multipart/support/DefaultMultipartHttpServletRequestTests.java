/*
 * Copyright 2002-2018 the original author or authors.
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
package org.springframework.web.multipart.support;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link DefaultMultipartHttpServletRequest}.
 *
 * @author Rossen Stoyanchev
 */
public class DefaultMultipartHttpServletRequestTests {

	private final MockHttpServletRequest servletRequest = new MockHttpServletRequest();

	private final Map<String, String[]> multipartParams = new LinkedHashMap<>();

	private final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();


	@Test // SPR-16590
	public void parameterValues() {

		this.multipartParams.put("key", new String[] {"p"});
		this.queryParams.add("key", "q");

		String[] values = createMultipartRequest().getParameterValues("key");

		assertArrayEquals(new String[] {"p", "q"}, values);
	}

	@Test // SPR-16590
	public void parameterMap() {

		this.multipartParams.put("key1", new String[] {"p1"});
		this.multipartParams.put("key2", new String[] {"p2"});

		this.queryParams.add("key1", "q1");
		this.queryParams.add("key3", "q3");

		Map<String, String[]> map = createMultipartRequest().getParameterMap();

		assertEquals(3, map.size());
		assertArrayEquals(new String[] {"p1", "q1"}, map.get("key1"));
		assertArrayEquals(new String[] {"p2"}, map.get("key2"));
		assertArrayEquals(new String[] {"q3"}, map.get("key3"));
	}

	private DefaultMultipartHttpServletRequest createMultipartRequest() {
		insertQueryParams();
		return new DefaultMultipartHttpServletRequest(this.servletRequest, new LinkedMultiValueMap<>(),
				this.multipartParams, new HashMap<>());
	}

	private void insertQueryParams() {
		StringBuilder query = new StringBuilder();
		for (String key : this.queryParams.keySet()) {
			for (String value : this.queryParams.get(key)) {
				this.servletRequest.addParameter(key, value);
				query.append(query.length() > 0 ? "&" : "").append(key).append("=").append(value);
			}
		}
		this.servletRequest.setQueryString(query.toString());
	}

}
