/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
	void processInjectionBasedOnServletContext() {
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
		assertThat(target.testBean).isInstanceOf(TestBean.class);
		assertThat(target.name).isEqualTo("tb");
	}


	public static class InjectionTarget {

		@Autowired
		public ITestBean testBean;

		@Value("#{testBean.name}")
		public String name;
	}

}
