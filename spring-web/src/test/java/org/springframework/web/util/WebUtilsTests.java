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

package org.springframework.web.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.mock.web.test.MockFilterChain;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.util.MultiValueMap;
import org.springframework.web.filter.ForwardedHeaderFilter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 */
public class WebUtilsTests {

	@Test
	public void findParameterValue() {
		Map<String, Object> params = new HashMap<>();
		params.put("myKey1", "myValue1");
		params.put("myKey2_myValue2", "xxx");
		params.put("myKey3_myValue3.x", "xxx");
		params.put("myKey4_myValue4.y", new String[] {"yyy"});

		assertThat(WebUtils.findParameterValue(params, "myKey0")).isNull();
		assertThat(WebUtils.findParameterValue(params, "myKey1")).isEqualTo("myValue1");
		assertThat(WebUtils.findParameterValue(params, "myKey2")).isEqualTo("myValue2");
		assertThat(WebUtils.findParameterValue(params, "myKey3")).isEqualTo("myValue3");
		assertThat(WebUtils.findParameterValue(params, "myKey4")).isEqualTo("myValue4");
	}

	@Test
	public void parseMatrixVariablesString() {
		MultiValueMap<String, String> variables;

		variables = WebUtils.parseMatrixVariables(null);
		assertThat(variables.size()).isEqualTo(0);

		variables = WebUtils.parseMatrixVariables("year");
		assertThat(variables.size()).isEqualTo(1);
		assertThat(variables.getFirst("year")).isEqualTo("");

		variables = WebUtils.parseMatrixVariables("year=2012");
		assertThat(variables.size()).isEqualTo(1);
		assertThat(variables.getFirst("year")).isEqualTo("2012");

		variables = WebUtils.parseMatrixVariables("year=2012;colors=red,blue,green");
		assertThat(variables.size()).isEqualTo(2);
		assertThat(variables.get("colors")).isEqualTo(Arrays.asList("red", "blue", "green"));
		assertThat(variables.getFirst("year")).isEqualTo("2012");

		variables = WebUtils.parseMatrixVariables(";year=2012;colors=red,blue,green;");
		assertThat(variables.size()).isEqualTo(2);
		assertThat(variables.get("colors")).isEqualTo(Arrays.asList("red", "blue", "green"));
		assertThat(variables.getFirst("year")).isEqualTo("2012");

		variables = WebUtils.parseMatrixVariables("colors=red;colors=blue;colors=green");
		assertThat(variables.size()).isEqualTo(1);
		assertThat(variables.get("colors")).isEqualTo(Arrays.asList("red", "blue", "green"));
	}

	@Test
	public void isValidOrigin() {
		List<String> allowed = Collections.emptyList();
		assertThat(checkValidOrigin("mydomain1.example", -1, "http://mydomain1.example", allowed)).isTrue();
		assertThat(checkValidOrigin("mydomain1.example", -1, "http://mydomain2.example", allowed)).isFalse();

		allowed = Collections.singletonList("*");
		assertThat(checkValidOrigin("mydomain1.example", -1, "http://mydomain2.example", allowed)).isTrue();

		allowed = Collections.singletonList("http://mydomain1.example");
		assertThat(checkValidOrigin("mydomain2.example", -1, "http://mydomain1.example", allowed)).isTrue();
		assertThat(checkValidOrigin("mydomain2.example", -1, "http://mydomain3.example", allowed)).isFalse();
	}

	@Test
	public void isSameOrigin() {
		assertThat(checkSameOrigin("http", "mydomain1.example", -1, "http://mydomain1.example")).isTrue();
		assertThat(checkSameOrigin("http", "mydomain1.example", -1, "http://mydomain1.example:80")).isTrue();
		assertThat(checkSameOrigin("https", "mydomain1.example", 443, "https://mydomain1.example")).isTrue();
		assertThat(checkSameOrigin("https", "mydomain1.example", 443, "https://mydomain1.example:443")).isTrue();
		assertThat(checkSameOrigin("http", "mydomain1.example", 123, "http://mydomain1.example:123")).isTrue();
		assertThat(checkSameOrigin("ws", "mydomain1.example", -1, "ws://mydomain1.example")).isTrue();
		assertThat(checkSameOrigin("wss", "mydomain1.example", 443, "wss://mydomain1.example")).isTrue();

		assertThat(checkSameOrigin("http", "mydomain1.example", -1, "http://mydomain2.example")).isFalse();
		assertThat(checkSameOrigin("http", "mydomain1.example", -1, "https://mydomain1.example")).isFalse();
		assertThat(checkSameOrigin("http", "mydomain1.example", -1, "invalid-origin")).isFalse();
		assertThat(checkSameOrigin("https", "mydomain1.example", -1, "http://mydomain1.example")).isFalse();

		// Handling of invalid origins as described in SPR-13478
		assertThat(checkSameOrigin("http", "mydomain1.example", -1, "http://mydomain1.example/")).isTrue();
		assertThat(checkSameOrigin("http", "mydomain1.example", -1, "http://mydomain1.example:80/")).isTrue();
		assertThat(checkSameOrigin("http", "mydomain1.example", -1, "http://mydomain1.example/path")).isTrue();
		assertThat(checkSameOrigin("http", "mydomain1.example", -1, "http://mydomain1.example:80/path")).isTrue();
		assertThat(checkSameOrigin("http", "mydomain2.example", -1, "http://mydomain1.example/")).isFalse();
		assertThat(checkSameOrigin("http", "mydomain2.example", -1, "http://mydomain1.example:80/")).isFalse();
		assertThat(checkSameOrigin("http", "mydomain2.example", -1, "http://mydomain1.example/path")).isFalse();
		assertThat(checkSameOrigin("http", "mydomain2.example", -1, "http://mydomain1.example:80/path")).isFalse();

		// Handling of IPv6 hosts as described in SPR-13525
		assertThat(checkSameOrigin("http", "[::1]", -1, "http://[::1]")).isTrue();
		assertThat(checkSameOrigin("http", "[::1]", 8080, "http://[::1]:8080")).isTrue();
		assertThat(checkSameOrigin("http",
				"[2001:0db8:0000:85a3:0000:0000:ac1f:8001]", -1,
				"http://[2001:0db8:0000:85a3:0000:0000:ac1f:8001]")).isTrue();
		assertThat(checkSameOrigin("http",
				"[2001:0db8:0000:85a3:0000:0000:ac1f:8001]", 8080,
				"http://[2001:0db8:0000:85a3:0000:0000:ac1f:8001]:8080")).isTrue();
		assertThat(checkSameOrigin("http", "[::1]", -1, "http://[::1]:8080")).isFalse();
		assertThat(checkSameOrigin("http", "[::1]", 8080,
				"http://[2001:0db8:0000:85a3:0000:0000:ac1f:8001]:8080")).isFalse();
	}

	@Test  // SPR-16262
	public void isSameOriginWithXForwardedHeaders() throws Exception {
		String server = "mydomain1.example";
		testWithXForwardedHeaders(server, -1, "https", null, -1, "https://mydomain1.example");
		testWithXForwardedHeaders(server, 123, "https", null, -1, "https://mydomain1.example");
		testWithXForwardedHeaders(server, -1, "https", "mydomain2.example", -1, "https://mydomain2.example");
		testWithXForwardedHeaders(server, 123, "https", "mydomain2.example", -1, "https://mydomain2.example");
		testWithXForwardedHeaders(server, -1, "https", "mydomain2.example", 456, "https://mydomain2.example:456");
		testWithXForwardedHeaders(server, 123, "https", "mydomain2.example", 456, "https://mydomain2.example:456");
	}

	@Test  // SPR-16262
	public void isSameOriginWithForwardedHeader() throws Exception {
		String server = "mydomain1.example";
		testWithForwardedHeader(server, -1, "proto=https", "https://mydomain1.example");
		testWithForwardedHeader(server, 123, "proto=https", "https://mydomain1.example");
		testWithForwardedHeader(server, -1, "proto=https; host=mydomain2.example", "https://mydomain2.example");
		testWithForwardedHeader(server, 123, "proto=https; host=mydomain2.example", "https://mydomain2.example");
		testWithForwardedHeader(server, -1, "proto=https; host=mydomain2.example:456", "https://mydomain2.example:456");
		testWithForwardedHeader(server, 123, "proto=https; host=mydomain2.example:456", "https://mydomain2.example:456");
	}


	private boolean checkValidOrigin(String serverName, int port, String originHeader, List<String> allowed) {
		MockHttpServletRequest servletRequest = new MockHttpServletRequest();
		ServerHttpRequest request = new ServletServerHttpRequest(servletRequest);
		servletRequest.setServerName(serverName);
		if (port != -1) {
			servletRequest.setServerPort(port);
		}
		servletRequest.addHeader(HttpHeaders.ORIGIN, originHeader);
		return WebUtils.isValidOrigin(request, allowed);
	}

	private boolean checkSameOrigin(String scheme, String serverName, int port, String originHeader) {
		MockHttpServletRequest servletRequest = new MockHttpServletRequest();
		ServerHttpRequest request = new ServletServerHttpRequest(servletRequest);
		servletRequest.setScheme(scheme);
		servletRequest.setServerName(serverName);
		if (port != -1) {
			servletRequest.setServerPort(port);
		}
		servletRequest.addHeader(HttpHeaders.ORIGIN, originHeader);
		return WebUtils.isSameOrigin(request);
	}

	private void testWithXForwardedHeaders(String serverName, int port, String forwardedProto,
			String forwardedHost, int forwardedPort, String originHeader) throws Exception {

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setServerName(serverName);
		if (port != -1) {
			request.setServerPort(port);
		}
		if (forwardedProto != null) {
			request.addHeader("X-Forwarded-Proto", forwardedProto);
		}
		if (forwardedHost != null) {
			request.addHeader("X-Forwarded-Host", forwardedHost);
		}
		if (forwardedPort != -1) {
			request.addHeader("X-Forwarded-Port", String.valueOf(forwardedPort));
		}
		request.addHeader(HttpHeaders.ORIGIN, originHeader);

		HttpServletRequest requestToUse = adaptFromForwardedHeaders(request);
		ServerHttpRequest httpRequest = new ServletServerHttpRequest(requestToUse);

		assertThat(WebUtils.isSameOrigin(httpRequest)).isTrue();
	}

	private void testWithForwardedHeader(String serverName, int port, String forwardedHeader,
			String originHeader) throws Exception {

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setServerName(serverName);
		if (port != -1) {
			request.setServerPort(port);
		}
		request.addHeader("Forwarded", forwardedHeader);
		request.addHeader(HttpHeaders.ORIGIN, originHeader);

		HttpServletRequest requestToUse = adaptFromForwardedHeaders(request);
		ServerHttpRequest httpRequest = new ServletServerHttpRequest(requestToUse);

		assertThat(WebUtils.isSameOrigin(httpRequest)).isTrue();
	}

	// SPR-16668
	private HttpServletRequest adaptFromForwardedHeaders(HttpServletRequest request) throws Exception {
		MockFilterChain chain = new MockFilterChain();
		new ForwardedHeaderFilter().doFilter(request, new MockHttpServletResponse(), chain);
		return (HttpServletRequest) chain.getRequest();
	}

}
