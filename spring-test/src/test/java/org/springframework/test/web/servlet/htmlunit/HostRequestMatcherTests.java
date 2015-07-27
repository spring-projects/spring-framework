/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.springframework.test.web.servlet.htmlunit;

import java.net.URL;

import org.junit.Test;

import com.gargoylesoftware.htmlunit.WebRequest;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Rob Winch
 * @since 4.2
 */
public class HostRequestMatcherTests {

	@Test
	public void localhostMatches() throws Exception {
		WebRequestMatcher matcher = new HostRequestMatcher("localhost");

		boolean matches = matcher.matches(new WebRequest(new URL("http://localhost/jquery-1.11.0.min.js")));
		assertThat(matches, equalTo(true));;

		matches = matcher.matches(new WebRequest(new URL("http://example.com/jquery-1.11.0.min.js")));
		assertThat(matches, equalTo(false));;
	}

	@Test
	public void multipleHosts() throws Exception {
		WebRequestMatcher matcher = new HostRequestMatcher("localhost","example.com");

		boolean matches = matcher.matches(new WebRequest(new URL("http://localhost/jquery-1.11.0.min.js")));
		assertThat(matches, equalTo(true));;

		matches = matcher.matches(new WebRequest(new URL("http://example.com/jquery-1.11.0.min.js")));
		assertThat(matches, equalTo(true));;
	}

	@Test
	public void specificPort() throws Exception {
		WebRequestMatcher matcher = new HostRequestMatcher("localhost:8080");

		boolean matches = matcher.matches(new WebRequest(new URL("http://localhost:8080/jquery-1.11.0.min.js")));
		assertThat(matches, equalTo(true));;

		matches = matcher.matches(new WebRequest(new URL("http://localhost:9090/jquery-1.11.0.min.js")));
		assertThat(matches, equalTo(false));;
	}

	@Test
	public void defaultPortInMatcher() throws Exception {
		WebRequestMatcher matcher = new HostRequestMatcher("localhost:80");

		boolean matches = matcher.matches(new WebRequest(new URL("http://localhost:80/jquery-1.11.0.min.js")));
		assertThat(matches, equalTo(true));;

		matches = matcher.matches(new WebRequest(new URL("http://localhost/jquery-1.11.0.min.js")));
		assertThat(matches, equalTo(true));;

		matches = matcher.matches(new WebRequest(new URL("http://localhost:9090/jquery-1.11.0.min.js")));
		assertThat(matches, equalTo(false));;
	}

}
