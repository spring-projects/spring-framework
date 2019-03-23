/*
 * Copyright 2002-2016 the original author or authors.
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
package org.springframework.web.servlet;

import java.io.IOException;
import javax.servlet.ServletException;

import org.junit.Test;

import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.mock.web.test.MockServletConfig;

/**
 * @author Rossen Stoyanchev
 */
public class ResourceServletTests {

	@Test(expected = ServletException.class)
	public void example1() throws Exception {
		testInvalidResourceUrl("/resources/**", "/resources/../WEB-INF/web.xml");
	}

	@Test(expected = ServletException.class)
	public void example2() throws Exception {
		testInvalidResourceUrl("/resources/*", "/resources/..\\WEB-INF\\web.xml");
	}

	@Test(expected = ServletException.class)
	public void example3() throws Exception {
		testInvalidResourceUrl("/resources/*", "/resources/..\\Servlet2?param=111");
	}

	private void testInvalidResourceUrl(String allowedResources, String resourceParam)
			throws ServletException, IOException {

		ResourceServlet servlet = new ResourceServlet();
		servlet.setAllowedResources(allowedResources);
		servlet.init(new MockServletConfig());

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
		request.addParameter("resource", resourceParam);
		MockHttpServletResponse response = new MockHttpServletResponse();

		servlet.service(request, response);
	}

}
