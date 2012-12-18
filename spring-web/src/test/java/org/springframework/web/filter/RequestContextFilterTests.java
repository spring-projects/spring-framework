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

package org.springframework.web.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import junit.framework.TestCase;

import org.springframework.mock.web.test.MockFilterConfig;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public class RequestContextFilterTests extends TestCase {

	public void testHappyPath() throws Exception {
		testFilterInvocation(null);
	}

	public void testWithException() throws Exception {
		testFilterInvocation(new ServletException());
	}

	public void testFilterInvocation(final ServletException sex) throws Exception {
		final MockHttpServletRequest req = new MockHttpServletRequest();
		req.setAttribute("myAttr", "myValue");
		final MockHttpServletResponse resp = new MockHttpServletResponse();

		// Expect one invocation by the filter being tested
		class DummyFilterChain implements FilterChain {
			public int invocations = 0;
			public void doFilter(ServletRequest req, ServletResponse resp) throws IOException, ServletException {
				++invocations;
				if (invocations == 1) {
					assertSame("myValue",
							RequestContextHolder.currentRequestAttributes().getAttribute("myAttr", RequestAttributes.SCOPE_REQUEST));
					if (sex != null) {
						throw sex;
					}
				}
				else {
					throw new IllegalStateException("Too many invocations");
				}
			}
		};

		DummyFilterChain fc = new DummyFilterChain();
		MockFilterConfig mfc = new MockFilterConfig(new MockServletContext(), "foo");

		RequestContextFilter rbf = new RequestContextFilter();
		rbf.init(mfc);

		try {
			rbf.doFilter(req, resp, fc);
			if (sex != null) {
				fail();
			}
		}
		catch (ServletException ex) {
			assertNotNull(sex);
		}

		try {
			RequestContextHolder.currentRequestAttributes();
			fail();
		}
		catch (IllegalStateException ex) {
			// Ok
		}

		assertEquals(1, fc.invocations);
	}

}
