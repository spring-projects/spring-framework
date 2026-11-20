/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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

import org.jspecify.annotations.Nullable;

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
