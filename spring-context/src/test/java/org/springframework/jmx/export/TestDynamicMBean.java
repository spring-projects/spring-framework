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

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.DynamicMBean;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;

import org.springframework.lang.Nullable;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
public class TestDynamicMBean implements DynamicMBean {

	public void setFailOnInit(boolean failOnInit) {
		if (failOnInit) {
			throw new IllegalArgumentException("Failing on initialization");
		}
	}

	@Override
	public Object getAttribute(String attribute) {
		if ("Name".equals(attribute)) {
			return "Rob Harrop";
		}
		return null;
	}

	@Override
	public void setAttribute(Attribute attribute) {
	}

	@Override
	public AttributeList getAttributes(String[] attributes) {
		return null;
	}

	@Override
	public AttributeList setAttributes(AttributeList attributes) {
		return null;
	}

	@Override
	public Object invoke(String actionName, Object[] params, String[] signature) {
		return null;
	}

	@Override
	public MBeanInfo getMBeanInfo() {
		MBeanAttributeInfo attr = new MBeanAttributeInfo("name", "java.lang.String", "", true, false, false);
		return new MBeanInfo(
				TestDynamicMBean.class.getName(), "",
				new MBeanAttributeInfo[]{attr},
				new MBeanConstructorInfo[0],
				new MBeanOperationInfo[0],
				new MBeanNotificationInfo[0]);
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		return (obj instanceof TestDynamicMBean);
	}

	@Override
	public int hashCode() {
		return TestDynamicMBean.class.hashCode();
	}

}
