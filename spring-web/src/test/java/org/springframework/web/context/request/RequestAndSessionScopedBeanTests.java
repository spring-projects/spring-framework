/*
 * Copyright 2002-2021 the original author or authors.
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
public class RequestAndSessionScopedBeanTests {

	@Test
	@SuppressWarnings("resource")
	public void testPutBeanInRequest() {
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
	@SuppressWarnings("resource")
	public void testPutBeanInSession() {
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
