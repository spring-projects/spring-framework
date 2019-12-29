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

package org.springframework.test.context.web;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that verify support for request and session scoped beans
 * in conjunction with the TestContext Framework.
 *
 * @author Sam Brannen
 * @since 3.2
 */
@SpringJUnitWebConfig
class RequestAndSessionScopedBeansWacTests {

	@Autowired
	WebApplicationContext wac;

	@Autowired
	MockHttpServletRequest request;

	@Autowired
	MockHttpSession session;


	@Test
	void requestScope() throws Exception {
		String beanName = "requestScopedTestBean";
		String contextPath = "/path";

		assertThat(request.getAttribute(beanName)).isNull();

		request.setContextPath(contextPath);
		TestBean testBean = wac.getBean(beanName, TestBean.class);

		assertThat(testBean.getName()).isEqualTo(contextPath);
		assertThat(request.getAttribute(beanName)).isSameAs(testBean);
		assertThat(wac.getBean(beanName, TestBean.class)).isSameAs(testBean);
	}

	@Test
	void sessionScope() throws Exception {
		String beanName = "sessionScopedTestBean";

		assertThat(session.getAttribute(beanName)).isNull();

		TestBean testBean = wac.getBean(beanName, TestBean.class);

		assertThat(session.getAttribute(beanName)).isSameAs(testBean);
		assertThat(wac.getBean(beanName, TestBean.class)).isSameAs(testBean);
	}

}
