/*
 * Copyright 2002-2019 the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.jmx.AbstractJmxTests;
import org.springframework.jmx.IJmxTestBean;
import org.springframework.jmx.support.ObjectNameManager;

import static org.assertj.core.api.Assertions.assertThat;

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
		assertThat(instance).as("Bean should not be null").isNotNull();
	}

	@Test
	public void testRegisterOperations() throws Exception {
		IJmxTestBean bean = getBean();
		assertThat(bean).isNotNull();
		MBeanInfo inf = getMBeanInfo();
		assertThat(inf.getOperations().length).as("Incorrect number of operations registered").isEqualTo(getExpectedOperationCount());
	}

	@Test
	public void testRegisterAttributes() throws Exception {
		IJmxTestBean bean = getBean();
		assertThat(bean).isNotNull();
		MBeanInfo inf = getMBeanInfo();
		assertThat(inf.getAttributes().length).as("Incorrect number of attributes registered").isEqualTo(getExpectedAttributeCount());
	}

	@Test
	public void testGetMBeanInfo() throws Exception {
		ModelMBeanInfo info = getMBeanInfoFromAssembler();
		assertThat(info).as("MBeanInfo should not be null").isNotNull();
	}

	@Test
	public void testGetMBeanAttributeInfo() throws Exception {
		ModelMBeanInfo info = getMBeanInfoFromAssembler();
		MBeanAttributeInfo[] inf = info.getAttributes();
		assertThat(inf.length).as("Invalid number of Attributes returned").isEqualTo(getExpectedAttributeCount());

		for (int x = 0; x < inf.length; x++) {
			assertThat(inf[x]).as("MBeanAttributeInfo should not be null").isNotNull();
			assertThat(inf[x].getDescription()).as("Description for MBeanAttributeInfo should not be null").isNotNull();
		}
	}

	@Test
	public void testGetMBeanOperationInfo() throws Exception {
		ModelMBeanInfo info = getMBeanInfoFromAssembler();
		MBeanOperationInfo[] inf = info.getOperations();
		assertThat(inf.length).as("Invalid number of Operations returned").isEqualTo(getExpectedOperationCount());

		for (int x = 0; x < inf.length; x++) {
			assertThat(inf[x]).as("MBeanOperationInfo should not be null").isNotNull();
			assertThat(inf[x].getDescription()).as("Description for MBeanOperationInfo should not be null").isNotNull();
		}
	}

	@Test
	public void testDescriptionNotNull() throws Exception {
		ModelMBeanInfo info = getMBeanInfoFromAssembler();

		assertThat(info.getDescription()).as("The MBean description should not be null").isNotNull();
	}

	@Test
	public void testSetAttribute() throws Exception {
		ObjectName objectName = ObjectNameManager.getInstance(getObjectName());
		getServer().setAttribute(objectName, new Attribute(NAME_ATTRIBUTE, "Rob Harrop"));
		IJmxTestBean bean = (IJmxTestBean) getContext().getBean("testBean");
		assertThat(bean.getName()).isEqualTo("Rob Harrop");
	}

	@Test
	public void testGetAttribute() throws Exception {
		ObjectName objectName = ObjectNameManager.getInstance(getObjectName());
		getBean().setName("John Smith");
		Object val = getServer().getAttribute(objectName, NAME_ATTRIBUTE);
		assertThat(val).as("Incorrect result").isEqualTo("John Smith");
	}

	@Test
	public void testOperationInvocation() throws Exception{
		ObjectName objectName = ObjectNameManager.getInstance(getObjectName());
		Object result = getServer().invoke(objectName, "add",
				new Object[] {new Integer(20), new Integer(30)}, new String[] {"int", "int"});
		assertThat(result).as("Incorrect result").isEqualTo(new Integer(50));
	}

	@Test
	public void testAttributeInfoHasDescriptors() throws Exception {
		ModelMBeanInfo info = getMBeanInfoFromAssembler();

		ModelMBeanAttributeInfo attr = info.getAttribute(NAME_ATTRIBUTE);
		Descriptor desc = attr.getDescriptor();
		assertThat(desc.getFieldValue("getMethod")).as("getMethod field should not be null").isNotNull();
		assertThat(desc.getFieldValue("setMethod")).as("setMethod field should not be null").isNotNull();
		assertThat(desc.getFieldValue("getMethod")).as("getMethod field has incorrect value").isEqualTo("getName");
		assertThat(desc.getFieldValue("setMethod")).as("setMethod field has incorrect value").isEqualTo("setName");
	}

	@Test
	public void testAttributeHasCorrespondingOperations() throws Exception {
		ModelMBeanInfo info = getMBeanInfoFromAssembler();

		ModelMBeanOperationInfo get = info.getOperation("getName");
		assertThat(get).as("get operation should not be null").isNotNull();
		assertThat(new Integer(4)).as("get operation should have visibility of four").isEqualTo(get.getDescriptor().getFieldValue("visibility"));
		assertThat(get.getDescriptor().getFieldValue("role")).as("get operation should have role \"getter\"").isEqualTo("getter");

		ModelMBeanOperationInfo set = info.getOperation("setName");
		assertThat(set).as("set operation should not be null").isNotNull();
		assertThat(new Integer(4)).as("set operation should have visibility of four").isEqualTo(set.getDescriptor().getFieldValue("visibility"));
		assertThat(set.getDescriptor().getFieldValue("role")).as("set operation should have role \"setter\"").isEqualTo("setter");
	}

	@Test
	public void testNotificationMetadata() throws Exception {
		ModelMBeanInfo info = (ModelMBeanInfo) getMBeanInfo();
		MBeanNotificationInfo[] notifications = info.getNotifications();
		assertThat(notifications.length).as("Incorrect number of notifications").isEqualTo(1);
		assertThat(notifications[0].getName()).as("Incorrect notification name").isEqualTo("My Notification");

		String[] notifTypes = notifications[0].getNotifTypes();

		assertThat(notifTypes.length).as("Incorrect number of notification types").isEqualTo(2);
		assertThat(notifTypes[0]).as("Notification type.foo not found").isEqualTo("type.foo");
		assertThat(notifTypes[1]).as("Notification type.bar not found").isEqualTo("type.bar");
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
