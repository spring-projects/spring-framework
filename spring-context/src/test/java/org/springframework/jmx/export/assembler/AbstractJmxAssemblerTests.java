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

package org.springframework.jmx.export.assembler;

import javax.management.Attribute;
import javax.management.Descriptor;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanInfo;
import javax.management.modelmbean.ModelMBeanOperationInfo;

import org.junit.jupiter.api.Test;

import org.springframework.jmx.AbstractJmxTests;
import org.springframework.jmx.ITestBean;
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
	void mBeanRegistration() throws Exception {
		// beans are registered at this point - just grab them from the server
		ObjectInstance instance = getObjectInstance();
		assertThat(instance).as("Bean should not be null").isNotNull();
	}

	@Test
	void registerOperations() throws Exception {
		assertThat(getBean()).isNotNull();
		MBeanInfo inf = getMBeanInfo();
		assertThat(inf.getOperations()).as("Incorrect number of operations registered").hasSize(getExpectedOperationCount());
	}

	@Test
	void registerAttributes() throws Exception {
		assertThat(getBean()).isNotNull();
		MBeanInfo inf = getMBeanInfo();
		assertThat(inf.getAttributes()).as("Incorrect number of attributes registered").hasSize(getExpectedAttributeCount());
	}

	@Test
	void getMBeanAttributeInfo() throws Exception {
		ModelMBeanInfo info = getMBeanInfoFromAssembler();
		assertThat(info).as("MBeanInfo should not be null").isNotNull();
		assertThat(info.getAttributes())
				.hasSize(getExpectedAttributeCount())
				.allSatisfy(element -> {
					assertThat(element).as("MBeanAttributeInfo should not be null").isNotNull();
					assertThat(element.getDescription()).as("Description for MBeanAttributeInfo should not be null").isNotNull();
				});
	}

	@Test
	void getMBeanOperationInfo() throws Exception {
		ModelMBeanInfo info = getMBeanInfoFromAssembler();
		assertThat(info).as("MBeanInfo should not be null").isNotNull();
		assertThat(info.getOperations())
				.hasSize(getExpectedOperationCount())
				.allSatisfy(element -> {
					assertThat(element).as("MBeanOperationInfo should not be null").isNotNull();
					assertThat(element.getDescription()).as("Description for MBeanOperationInfo should not be null").isNotNull();
				});
	}

	@Test
	void descriptionNotNull() throws Exception {
		ModelMBeanInfo info = getMBeanInfoFromAssembler();

		assertThat(info.getDescription()).as("The MBean description should not be null").isNotNull();
	}

	@Test
	void setAttribute() throws Exception {
		ObjectName objectName = ObjectNameManager.getInstance(getObjectName());
		getServer().setAttribute(objectName, new Attribute(NAME_ATTRIBUTE, "Rob Harrop"));
		assertThat(getBean().getName()).isEqualTo("Rob Harrop");
	}

	@Test
	void getAttribute() throws Exception {
		ObjectName objectName = ObjectNameManager.getInstance(getObjectName());
		getBean().setName("John Smith");
		Object val = getServer().getAttribute(objectName, NAME_ATTRIBUTE);
		assertThat(val).as("Incorrect result").isEqualTo("John Smith");
	}

	@Test
	void operationInvocation() throws Exception{
		ObjectName objectName = ObjectNameManager.getInstance(getObjectName());
		Object result = getServer().invoke(objectName, "add",
				new Object[] {20, 30}, new String[] {"int", "int"});
		assertThat(result).as("Incorrect result").isEqualTo(50);
	}

	@Test
	void attributeInfoHasDescriptors() throws Exception {
		ModelMBeanInfo info = getMBeanInfoFromAssembler();

		ModelMBeanAttributeInfo attr = info.getAttribute(NAME_ATTRIBUTE);
		Descriptor desc = attr.getDescriptor();
		assertThat(desc.getFieldValue("getMethod")).as("getMethod field should not be null").isNotNull();
		assertThat(desc.getFieldValue("setMethod")).as("setMethod field should not be null").isNotNull();
		assertThat(desc.getFieldValue("getMethod")).as("getMethod field has incorrect value").isEqualTo("getName");
		assertThat(desc.getFieldValue("setMethod")).as("setMethod field has incorrect value").isEqualTo("setName");
	}

	@Test
	void attributeHasCorrespondingOperations() throws Exception {
		ModelMBeanInfo info = getMBeanInfoFromAssembler();

		ModelMBeanOperationInfo get = info.getOperation("getName");
		assertThat(get).as("get operation should not be null").isNotNull();
		assertThat(get.getDescriptor().getFieldValue("visibility")).as("get operation should have visibility of four").isEqualTo(4);
		assertThat(get.getDescriptor().getFieldValue("role")).as("get operation should have role \"getter\"").isEqualTo("getter");

		ModelMBeanOperationInfo set = info.getOperation("setName");
		assertThat(set).as("set operation should not be null").isNotNull();
		assertThat(set.getDescriptor().getFieldValue("visibility")).as("set operation should have visibility of four").isEqualTo(4);
		assertThat(set.getDescriptor().getFieldValue("role")).as("set operation should have role \"setter\"").isEqualTo("setter");
	}

	@Test
	void notificationMetadata() throws Exception {
		ModelMBeanInfo info = (ModelMBeanInfo) getMBeanInfo();
		MBeanNotificationInfo[] notifications = info.getNotifications();
		assertThat(notifications).as("Incorrect number of notifications").hasSize(1);
		assertThat(notifications[0].getName()).as("Incorrect notification name").isEqualTo("My Notification");
		assertThat(notifications[0].getNotifTypes()).as("notification types").containsExactly("type.foo", "type.bar");
	}

	protected ModelMBeanInfo getMBeanInfoFromAssembler() throws Exception {
		return getAssembler().getMBeanInfo(getBean(), getObjectName());
	}

	protected ITestBean getBean() {
		return getContext().getBean("testBean", ITestBean.class);
	}

	protected MBeanInfo getMBeanInfo() throws Exception {
		return getServer().getMBeanInfo(ObjectNameManager.getInstance(getObjectName()));
	}

	protected ObjectInstance getObjectInstance() throws Exception {
		return getServer().getObjectInstance(ObjectNameManager.getInstance(getObjectName()));
	}

	protected abstract int getExpectedOperationCount();

	protected abstract int getExpectedAttributeCount();

	protected abstract MBeanInfoAssembler getAssembler();

}
