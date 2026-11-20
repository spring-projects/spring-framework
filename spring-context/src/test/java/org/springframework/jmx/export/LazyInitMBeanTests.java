/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jmx.export;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.jupiter.api.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jmx.support.ObjectNameManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
class LazyInitMBeanTests {

	@Test
	void invokeOnLazyInitBean() throws Exception {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("org/springframework/jmx/export/lazyInit.xml");
		try (ctx) {
			assertThat(ctx.getBeanFactory().containsSingleton("testBean")).isFalse();
			assertThat(ctx.getBeanFactory().containsSingleton("testBean2")).isFalse();
			MBeanServer server = (MBeanServer) ctx.getBean("server");
			ObjectName oname = ObjectNameManager.getInstance("bean:name=testBean2");
			String name = (String) server.getAttribute(oname, "Name");
			assertThat(name).as("Invalid name returned").isEqualTo("foo");
		}
	}

}
