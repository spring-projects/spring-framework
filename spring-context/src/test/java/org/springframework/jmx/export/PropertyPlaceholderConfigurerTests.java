/*
 * Copyright 2002-2005 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.springframework.jmx.export;

import javax.management.ObjectName;

import org.springframework.jmx.AbstractJmxTests;
import org.springframework.jmx.IJmxTestBean;

/**
 * @author Rob Harrop
 */
public class PropertyPlaceholderConfigurerTests extends AbstractJmxTests {

	@Override
	protected String getApplicationContextPath() {
		return "org/springframework/jmx/export/propertyPlaceholderConfigurer.xml";
	}

	public void testPropertiesReplaced() {
		IJmxTestBean bean = (IJmxTestBean) getContext().getBean("testBean");

		assertEquals("Name is incorrect", "Rob Harrop", bean.getName());
		assertEquals("Age is incorrect", 100, bean.getAge());
	}

	public void testPropertiesCorrectInJmx() throws Exception {
		ObjectName oname = new ObjectName("bean:name=proxyTestBean1");
		Object name = getServer().getAttribute(oname, "Name");
		Integer age = (Integer) getServer().getAttribute(oname, "Age");

		assertEquals("Name is incorrect in JMX", "Rob Harrop", name);
		assertEquals("Age is incorrect in JMX", 100, age.intValue());
	}

}

