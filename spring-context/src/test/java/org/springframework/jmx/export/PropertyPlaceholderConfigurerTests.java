/*
 * Copyright 2002-2023 the original author or authors.
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
