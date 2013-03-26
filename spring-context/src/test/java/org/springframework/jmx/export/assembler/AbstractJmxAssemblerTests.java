/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.jmx.export.assembler;

import javax.management.Attribute;
import javax.management.Descriptor;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanInfo;
import javax.management.modelmbean.ModelMBeanOperationInfo;

import org.junit.Test;
import org.springframework.jmx.AbstractJmxTests;
import org.springframework.jmx.IJmxTestBean;
import org.springframework.jmx.support.ObjectNameManager;

import static org.junit.Assert.*;

/**
 * @author Rob Harrop
 * @author Chris Beams
 */
public abstract class AbstractJmxAssemblerTests extends AbstractJmxTests {

	protected static final String AGE_ATTRIBUTE = "Age";

	protected static final String NAME_ATTRIBUTE = "Name";

	protected abstract String getObjectName();

	@Test
	public void testMBeanRegistration() throws Exception {
		// beans are registered at this point - just grab them from the server
		ObjectInstance instance = getObjectInstance();
		assertNotNull("Bean should not be null", instance);
	}

	@Test
	public void testRegisterOperations() throws Exception {
		IJmxTestBean bean = getBean();
		assertNotNull(bean);
		MBeanInfo inf = getMBeanInfo();
		assertEquals("Incorrect number of operations registered",
				getExpectedOperationCount(), inf.getOperations().length);
	}

	@Test
	public void testRegisterAttributes() throws Exception {
		IJmxTestBean bean = getBean();
		assertNotNull(bean);
		MBeanInfo inf = getMBeanInfo();
		assertEquals("Incorrect number of attributes registered",
				getExpectedAttributeCount(), inf.getAttributes().length);
	}

	@Test
	public void testGetMBeanInfo() throws Exception {
		ModelMBeanInfo info = getMBeanInfoFromAssembler();
		assertNotNull("MBeanInfo should not be null", info);
	}

	@Test
	public void testGetMBeanAttributeInfo() throws Exception {
		ModelMBeanInfo info = getMBeanInfoFromAssembler();
		MBeanAttributeInfo[] inf = info.getAttributes();
		assertEquals("Invalid number of Attributes returned",
				getExpectedAttributeCount(), inf.length);

		for (int x = 0; x < inf.length; x++) {
			assertNotNull("MBeanAttributeInfo should not be null", inf[x]);
			assertNotNull(
					"Description for MBeanAttributeInfo should not be null",
					inf[x].getDescription());
		}
	}

	@Test
	public void testGetMBeanOperationInfo() throws Exception {
		ModelMBeanInfo info = getMBeanInfoFromAssembler();
		MBeanOperationInfo[] inf = info.getOperations();
		assertEquals("Invalid number of Operations returned",
				getExpectedOperationCount(), inf.length);

		for (int x = 0; x < inf.length; x++) {
			assertNotNull("MBeanOperationInfo should not be null", inf[x]);
			assertNotNull(
					"Description for MBeanOperationInfo should not be null",
					inf[x].getDescription());
		}
	}

	@Test
	public void testDescriptionNotNull() throws Exception {
		ModelMBeanInfo info = getMBeanInfoFromAssembler();

		assertNotNull("The MBean description should not be null",
				info.getDescription());
	}

	@Test
	public void testSetAttribute() throws Exception {
		ObjectName objectName = ObjectNameManager.getInstance(getObjectName());
		getServer().setAttribute(objectName, new Attribute(NAME_ATTRIBUTE, "Rob Harrop"));
		IJmxTestBean bean = (IJmxTestBean) getContext().getBean("testBean");
		assertEquals("Rob Harrop", bean.getName());
	}

	@Test
	public void testGetAttribute() throws Exception {
		ObjectName objectName = ObjectNameManager.getInstance(getObjectName());
		getBean().setName("John Smith");
		Object val = getServer().getAttribute(objectName, NAME_ATTRIBUTE);
		assertEquals("Incorrect result", "John Smith", val);
	}

	@Test
	public void testOperationInvocation() throws Exception{
		ObjectName objectName = ObjectNameManager.getInstance(getObjectName());
		Object result = getServer().invoke(objectName, "add",
				new Object[] {new Integer(20), new Integer(30)}, new String[] {"int", "int"});
	assertEquals("Incorrect result", new Integer(50), result);
	}

	@Test
	public void testAttributeInfoHasDescriptors() throws Exception {
		ModelMBeanInfo info = getMBeanInfoFromAssembler();

		ModelMBeanAttributeInfo attr = info.getAttribute(NAME_ATTRIBUTE);
		Descriptor desc = attr.getDescriptor();
		assertNotNull("getMethod field should not be null",
				desc.getFieldValue("getMethod"));
		assertNotNull("setMethod field should not be null",
				desc.getFieldValue("setMethod"));
		assertEquals("getMethod field has incorrect value", "getName",
				desc.getFieldValue("getMethod"));
		assertEquals("setMethod field has incorrect value", "setName",
				desc.getFieldValue("setMethod"));
	}

	@Test
	public void testAttributeHasCorrespondingOperations() throws Exception {
		ModelMBeanInfo info = getMBeanInfoFromAssembler();

		ModelMBeanOperationInfo get = info.getOperation("getName");
		assertNotNull("get operation should not be null", get);
		assertEquals("get operation should have visibility of four",
				get.getDescriptor().getFieldValue("visibility"),
				new Integer(4));
		assertEquals("get operation should have role \"getter\"", "getter", get.getDescriptor().getFieldValue("role"));

		ModelMBeanOperationInfo set = info.getOperation("setName");
		assertNotNull("set operation should not be null", set);
		assertEquals("set operation should have visibility of four",
				set.getDescriptor().getFieldValue("visibility"),
				new Integer(4));
		assertEquals("set operation should have role \"setter\"", "setter", set.getDescriptor().getFieldValue("role"));
	}

	@Test
	public void testNotificationMetadata() throws Exception {
		ModelMBeanInfo info = (ModelMBeanInfo) getMBeanInfo();
		MBeanNotificationInfo[] notifications = info.getNotifications();
		assertEquals("Incorrect number of notifications", 1, notifications.length);
		assertEquals("Incorrect notification name", "My Notification", notifications[0].getName());

		String[] notifTypes = notifications[0].getNotifTypes();

		assertEquals("Incorrect number of notification types", 2, notifTypes.length);
		assertEquals("Notification type.foo not found", "type.foo", notifTypes[0]);
		assertEquals("Notification type.bar not found", "type.bar", notifTypes[1]);
	}

	protected ModelMBeanInfo getMBeanInfoFromAssembler() throws Exception {
		IJmxTestBean bean = getBean();
		ModelMBeanInfo info = getAssembler().getMBeanInfo(bean, getObjectName());
		return info;
	}

	protected IJmxTestBean getBean() {
		Object bean = getContext().getBean("testBean");
		return (IJmxTestBean) bean;
	}

	protected MBeanInfo getMBeanInfo() throws Exception {
		return getServer().getMBeanInfo(ObjectNameManager.getInstance(getObjectName()));
	}

	protected ObjectInstance getObjectInstance() throws Exception {
		return getServer().getObjectInstance(ObjectNameManager.getInstance(getObjectName()));
	}

	protected abstract int getExpectedOperationCount();

	protected abstract int getExpectedAttributeCount();

	protected abstract MBeanInfoAssembler getAssembler() throws Exception;

}
