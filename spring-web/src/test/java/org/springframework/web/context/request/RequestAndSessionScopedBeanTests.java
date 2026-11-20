/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.context.request;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
class RequestAndSessionScopedBeanTests {

	@Test
	void putBeanInRequest() {
		String targetBeanName = "target";

		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		bd.setScope(WebApplicationContext.SCOPE_REQUEST);
		bd.getPropertyValues().add("name", "abc");
		wac.registerBeanDefinition(targetBeanName, bd);
		wac.refresh();

		HttpServletRequest request = new MockHttpServletRequest();
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
		TestBean target = (TestBean) wac.getBean(targetBeanName);
		assertThat(target.getName()).isEqualTo("abc");
		assertThat(request.getAttribute(targetBeanName)).isSameAs(target);

		TestBean target2 = (TestBean) wac.getBean(targetBeanName);
		assertThat(target2.getName()).isEqualTo("abc");
		assertThat(target).isSameAs(target2);
		assertThat(request.getAttribute(targetBeanName)).isSameAs(target2);

		request = new MockHttpServletRequest();
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
		TestBean target3 = (TestBean) wac.getBean(targetBeanName);
		assertThat(target3.getName()).isEqualTo("abc");
		assertThat(request.getAttribute(targetBeanName)).isSameAs(target3);
		assertThat(target).isNotSameAs(target3);

		RequestContextHolder.setRequestAttributes(null);
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
				wac.getBean(targetBeanName));
	}

	@Test
	void putBeanInSession() {
		String targetBeanName = "target";
		HttpServletRequest request = new MockHttpServletRequest();
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		bd.setScope(WebApplicationContext.SCOPE_SESSION);
		bd.getPropertyValues().add("name", "abc");
		wac.registerBeanDefinition(targetBeanName, bd);
		wac.refresh();

		TestBean target = (TestBean) wac.getBean(targetBeanName);
		assertThat(target.getName()).isEqualTo("abc");
		assertThat(request.getSession().getAttribute(targetBeanName)).isSameAs(target);

		RequestContextHolder.setRequestAttributes(null);
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
				wac.getBean(targetBeanName));
	}

}
