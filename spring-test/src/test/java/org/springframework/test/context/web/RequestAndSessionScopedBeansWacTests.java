/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.test.context.web;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.tests.sample.beans.TestBean;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.Assert.*;

/**
 * Integration tests that verify support for request and session scoped beans
 * in conjunction with the TestContext Framework.
 *
 * @author Sam Brannen
 * @since 3.2
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@WebAppConfiguration
public class RequestAndSessionScopedBeansWacTests {

	@Autowired
	private WebApplicationContext wac;

	@Autowired
	private MockHttpServletRequest request;

	@Autowired
	private MockHttpSession session;


	@Test
	public void requestScope() throws Exception {
		final String beanName = "requestScopedTestBean";
		final String contextPath = "/path";

		assertNull(request.getAttribute(beanName));

		request.setContextPath(contextPath);
		TestBean testBean = wac.getBean(beanName, TestBean.class);

		assertEquals(contextPath, testBean.getName());
		assertSame(testBean, request.getAttribute(beanName));
		assertSame(testBean, wac.getBean(beanName, TestBean.class));
	}

	@Test
	public void sessionScope() throws Exception {
		final String beanName = "sessionScopedTestBean";

		assertNull(session.getAttribute(beanName));

		TestBean testBean = wac.getBean(beanName, TestBean.class);

		assertSame(testBean, session.getAttribute(beanName));
		assertSame(testBean, wac.getBean(beanName, TestBean.class));
	}

}
