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

package org.springframework.web.context.support;

import org.junit.jupiter.api.Test;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.testfixture.beans.ITestBean;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.testfixture.servlet.MockServletContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 */
class SpringBeanAutowiringSupportTests {

	@Test
	void testProcessInjectionBasedOnServletContext() {
		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		AnnotationConfigUtils.registerAnnotationConfigProcessors(wac);

		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("name", "tb");
		wac.registerSingleton("testBean", TestBean.class, pvs);

		MockServletContext sc = new MockServletContext();
		wac.setServletContext(sc);
		wac.refresh();
		sc.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);

		InjectionTarget target = new InjectionTarget();
		SpringBeanAutowiringSupport.processInjectionBasedOnServletContext(target, sc);
		boolean condition = target.testBean instanceof TestBean;
		assertThat(condition).isTrue();
		assertThat(target.name).isEqualTo("tb");
	}


	public static class InjectionTarget {

		@Autowired
		public ITestBean testBean;

		@Value("#{testBean.name}")
		public String name;
	}

}
