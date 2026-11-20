/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
	void requestScope() {
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
	void sessionScope() {
		String beanName = "sessionScopedTestBean";

		assertThat(session.getAttribute(beanName)).isNull();

		TestBean testBean = wac.getBean(beanName, TestBean.class);

		assertThat(session.getAttribute(beanName)).isSameAs(testBean);
		assertThat(wac.getBean(beanName, TestBean.class)).isSameAs(testBean);
	}

}
