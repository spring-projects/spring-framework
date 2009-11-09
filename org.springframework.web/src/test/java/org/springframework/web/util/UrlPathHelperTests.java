/*
 * Copyright 2002-2007 the original author or authors.
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

import java.net.URI;
import java.net.URISyntaxException;

import junit.framework.TestCase;

import org.springframework.mock.web.MockHttpServletRequest;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.assertEquals;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
public class UrlPathHelperTests {

	private UrlPathHelper helper;

	private MockHttpServletRequest request;

	@Before
	public void setUp() {
		helper = new UrlPathHelper();
		request = new MockHttpServletRequest();
	}

	@Test
	public void getPathWithinApplication() {
		request.setContextPath("/petclinic");
		request.setRequestURI("/petclinic/welcome.html");

		assertEquals("Incorrect path returned", "/welcome.html", helper.getPathWithinApplication(request));
	}

	@Test
	public void getPathWithinApplicationForRootWithNoLeadingSlash() {
		request.setContextPath("/petclinic");
		request.setRequestURI("/petclinic");

		assertEquals("Incorrect root path returned", "/", helper.getPathWithinApplication(request));
	}

	@Test
	public void getPathWithinApplicationForSlashContextPath() {
		request.setContextPath("/");
		request.setRequestURI("/welcome.html");

		assertEquals("Incorrect path returned", "/welcome.html", helper.getPathWithinApplication(request));
	}

	@Test
	public void getRequestUri() {
		request.setRequestURI("/welcome.html");
		assertEquals("Incorrect path returned", "/welcome.html", helper.getRequestUri(request));

		request.setRequestURI("/foo%20bar");
		assertEquals("Incorrect path returned", "/foo bar", helper.getRequestUri(request));

		request.setRequestURI("/foo+bar");
		assertEquals("Incorrect path returned", "/foo+bar", helper.getRequestUri(request));

	}

}
