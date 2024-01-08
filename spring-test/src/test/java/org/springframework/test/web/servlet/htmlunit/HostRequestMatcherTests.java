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

package org.springframework.test.web.servlet.htmlunit;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link HostRequestMatcher}.
 *
 * @author Rob Winch
 * @author Sam Brannen
 * @since 4.2
 */
public class HostRequestMatcherTests extends AbstractWebRequestMatcherTests {

	@Test
	public void localhost() throws Exception {
		WebRequestMatcher matcher = new HostRequestMatcher("localhost");
		assertMatches(matcher, "http://localhost/jquery-1.11.0.min.js");
		assertDoesNotMatch(matcher, "http://company.example/jquery-1.11.0.min.js");
	}

	@Test
	public void multipleHosts() throws Exception {
		WebRequestMatcher matcher = new HostRequestMatcher("localhost", "example.com");
		assertMatches(matcher, "http://localhost/jquery-1.11.0.min.js");
		assertMatches(matcher, "https://example.com/jquery-1.11.0.min.js");
	}

	@Test
	public void specificPort() throws Exception {
		WebRequestMatcher matcher = new HostRequestMatcher("localhost:8080");
		assertMatches(matcher, "http://localhost:8080/jquery-1.11.0.min.js");
		assertDoesNotMatch(matcher, "http://localhost:9090/jquery-1.11.0.min.js");
	}

	@Test
	public void defaultHttpPort() throws Exception {
		WebRequestMatcher matcher = new HostRequestMatcher("localhost:80");
		assertMatches(matcher, "http://localhost:80/jquery-1.11.0.min.js");
		assertMatches(matcher, "http://localhost/jquery-1.11.0.min.js");
		assertDoesNotMatch(matcher, "https://localhost/jquery-1.11.0.min.js");
		assertDoesNotMatch(matcher, "http://localhost:9090/jquery-1.11.0.min.js");
	}

	@Test
	public void defaultHttpsPort() throws Exception {
		WebRequestMatcher matcher = new HostRequestMatcher("localhost:443");
		assertMatches(matcher, "https://localhost:443/jquery-1.11.0.min.js");
		assertMatches(matcher, "https://localhost/jquery-1.11.0.min.js");
		assertDoesNotMatch(matcher, "http://localhost/jquery-1.11.0.min.js");
		assertDoesNotMatch(matcher, "https://localhost:9090/jquery-1.11.0.min.js");
	}

}
