/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jmx.export;

import javax.management.ObjectName;

import org.junit.jupiter.api.Test;

import org.springframework.jmx.AbstractJmxTests;
import org.springframework.jmx.IJmxTestBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 * @author Chris Beams
 */
class PropertyPlaceholderConfigurerTests extends AbstractJmxTests {

	@Override
	protected String getApplicationContextPath() {
		return "org/springframework/jmx/export/propertyPlaceholderConfigurer.xml";
	}

	@Test
	void propertiesReplacedInApplicationContext() {
		IJmxTestBean bean = getContext().getBean("testBean", IJmxTestBean.class);

		assertThat(bean.getName()).as("Name").isEqualTo("Rob Harrop");
		assertThat(bean.getAge()).as("Age").isEqualTo(100);
	}

	@Test
	void propertiesCorrectInJmx() throws Exception {
		ObjectName oname = new ObjectName("bean:name=proxyTestBean1");
		Object name = getServer().getAttribute(oname, "Name");
		Integer age = (Integer) getServer().getAttribute(oname, "Age");

		assertThat(name).as("Name is incorrect in JMX").isEqualTo("Rob Harrop");
		assertThat(age).as("Age is incorrect in JMX").isEqualTo(100);
	}

}
