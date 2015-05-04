/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.util;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.util.MultiValueMap;

/**
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 */
public class WebUtilsTests {

	@Test
	public void findParameterValue() {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("myKey1", "myValue1");
		params.put("myKey2_myValue2", "xxx");
		params.put("myKey3_myValue3.x", "xxx");
		params.put("myKey4_myValue4.y", new String[] {"yyy"});

		assertNull(WebUtils.findParameterValue(params, "myKey0"));
		assertEquals("myValue1", WebUtils.findParameterValue(params, "myKey1"));
		assertEquals("myValue2", WebUtils.findParameterValue(params, "myKey2"));
		assertEquals("myValue3", WebUtils.findParameterValue(params, "myKey3"));
		assertEquals("myValue4", WebUtils.findParameterValue(params, "myKey4"));
	}

	@Test
	public void extractFilenameFromUrlPath() {
		assertEquals("index", WebUtils.extractFilenameFromUrlPath("index.html"));
		assertEquals("index", WebUtils.extractFilenameFromUrlPath("/index.html"));
		assertEquals("view", WebUtils.extractFilenameFromUrlPath("/products/view.html"));
		assertEquals("view", WebUtils.extractFilenameFromUrlPath("/products/view.html?param=a"));
		assertEquals("view", WebUtils.extractFilenameFromUrlPath("/products/view.html?param=/path/a"));
		assertEquals("view", WebUtils.extractFilenameFromUrlPath("/products/view.html?param=/path/a.do"));
	}

	@Test
	public void extractFullFilenameFromUrlPath() {
		assertEquals("index.html", WebUtils.extractFullFilenameFromUrlPath("index.html"));
		assertEquals("index.html", WebUtils.extractFullFilenameFromUrlPath("/index.html"));
		assertEquals("view.html", WebUtils.extractFullFilenameFromUrlPath("/products/view.html"));
		assertEquals("view.html", WebUtils.extractFullFilenameFromUrlPath("/products/view.html?param=a"));
		assertEquals("view.html", WebUtils.extractFullFilenameFromUrlPath("/products/view.html?param=/path/a"));
		assertEquals("view.html", WebUtils.extractFullFilenameFromUrlPath("/products/view.html?param=/path/a.do"));
	}

	@Test
	public void parseMatrixVariablesString() {
		MultiValueMap<String, String> variables;

		variables = WebUtils.parseMatrixVariables(null);
		assertEquals(0, variables.size());

		variables = WebUtils.parseMatrixVariables("year");
		assertEquals(1, variables.size());
		assertEquals("", variables.getFirst("year"));

		variables = WebUtils.parseMatrixVariables("year=2012");
		assertEquals(1, variables.size());
		assertEquals("2012", variables.getFirst("year"));

		variables = WebUtils.parseMatrixVariables("year=2012;colors=red,blue,green");
		assertEquals(2, variables.size());
		assertEquals(Arrays.asList("red", "blue", "green"), variables.get("colors"));
		assertEquals("2012", variables.getFirst("year"));

		variables = WebUtils.parseMatrixVariables(";year=2012;colors=red,blue,green;");
		assertEquals(2, variables.size());
		assertEquals(Arrays.asList("red", "blue", "green"), variables.get("colors"));
		assertEquals("2012", variables.getFirst("year"));

		variables = WebUtils.parseMatrixVariables("colors=red;colors=blue;colors=green");
		assertEquals(1, variables.size());
		assertEquals(Arrays.asList("red", "blue", "green"), variables.get("colors"));
	}

	@Test
	public void isValidOriginSuccess() {

		List<String> allowed = Collections.emptyList();
		assertTrue(checkOrigin("mydomain1.com", -1, "http://mydomain1.com", allowed));
		assertTrue(checkOrigin("mydomain1.com", -1, "http://mydomain1.com:80", allowed));
		assertTrue(checkOrigin("mydomain1.com", 443, "https://mydomain1.com", allowed));
		assertTrue(checkOrigin("mydomain1.com", 443, "https://mydomain1.com:443", allowed));
		assertTrue(checkOrigin("mydomain1.com", 123, "http://mydomain1.com:123", allowed));
		assertTrue(checkOrigin("mydomain1.com", -1, "ws://mydomain1.com", allowed));
		assertTrue(checkOrigin("mydomain1.com", 443, "wss://mydomain1.com", allowed));

		allowed = Collections.singletonList("*");
		assertTrue(checkOrigin("mydomain1.com", -1, "http://mydomain2.com", allowed));

		allowed = Collections.singletonList("http://mydomain1.com");
		assertTrue(checkOrigin("mydomain2.com", -1, "http://mydomain1.com", allowed));
	}

	@Test
	public void isValidOriginFailure() {

		List<String> allowed = Collections.emptyList();
		assertFalse(checkOrigin("mydomain1.com", -1, "http://mydomain2.com", allowed));
		assertFalse(checkOrigin("mydomain1.com", -1, "https://mydomain1.com", allowed));
		assertFalse(checkOrigin("mydomain1.com", -1, "invalid-origin", allowed));

		allowed = Collections.singletonList("http://mydomain1.com");
		assertFalse(checkOrigin("mydomain2.com", -1, "http://mydomain3.com", allowed));
	}

	private boolean checkOrigin(String serverName, int port, String originHeader, List<String> allowed) {
		MockHttpServletRequest servletRequest = new MockHttpServletRequest();
		ServerHttpRequest request = new ServletServerHttpRequest(servletRequest);
		servletRequest.setServerName(serverName);
		if (port != -1) {
			servletRequest.setServerPort(port);
		}
		request.getHeaders().set(HttpHeaders.ORIGIN, originHeader);
		return WebUtils.isValidOrigin(request, allowed);
	}

}
