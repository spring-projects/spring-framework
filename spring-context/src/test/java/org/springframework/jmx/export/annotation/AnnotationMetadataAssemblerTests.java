/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jmx.export.annotation;

import javax.management.MBeanNotificationInfo;
import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanInfo;
import javax.management.modelmbean.ModelMBeanOperationInfo;

import org.junit.jupiter.api.Test;

import org.springframework.jmx.ITestBean;
import org.springframework.jmx.export.assembler.AbstractMetadataAssemblerTests;
import org.springframework.jmx.export.metadata.JmxAttributeSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 * @author Chris Beams
 */
class AnnotationMetadataAssemblerTests extends AbstractMetadataAssemblerTests {

	private static final String OBJECT_NAME = "bean:name=testBean4";

	@Test
	@Override
	protected void notificationMetadata() throws Exception {
		ModelMBeanInfo info = (ModelMBeanInfo) getMBeanInfo();
		MBeanNotificationInfo[] notifications = info.getNotifications();
		assertThat(notifications).as("Incorrect number of notifications").hasSize(2);
		assertThat(notifications[0].getName()).as("Incorrect notification name").isEqualTo("My Notification 1");
		assertThat(notifications[0].getNotifTypes()).as("notification types").containsExactly("type.foo", "type.bar");
		assertThat(notifications[1].getName()).as("Incorrect notification name").isEqualTo("My Notification 2");
		assertThat(notifications[1].getNotifTypes()).as("notification types").containsExactly("type.enigma");
	}

	@Test
	void attributeFromInterface() throws Exception {
		ModelMBeanInfo inf = getMBeanInfoFromAssembler();
		ModelMBeanAttributeInfo attr = inf.getAttribute("Colour");
		assertThat(attr.isWritable()).as("The name attribute should be writable").isTrue();
		assertThat(attr.isReadable()).as("The name attribute should be readable").isTrue();
	}

	@Test
	void operationFromInterface() throws Exception {
		ModelMBeanInfo inf = getMBeanInfoFromAssembler();
		ModelMBeanOperationInfo op = inf.getOperation("fromInterface");
		assertThat(op).isNotNull();
	}

	@Test
	void operationOnGetter() throws Exception {
		ModelMBeanInfo inf = getMBeanInfoFromAssembler();
		ModelMBeanOperationInfo op = inf.getOperation("getExpensiveToCalculate");
		assertThat(op).isNotNull();
	}

	@Test
	void registrationOnInterface() throws Exception {
		Object bean = getContext().getBean("testInterfaceBean");
		ModelMBeanInfo inf = getAssembler().getMBeanInfo(bean, "bean:name=interfaceTestBean");
		assertThat(inf).isNotNull();
		assertThat(inf.getDescription()).isEqualTo("My Managed Bean");

		ModelMBeanOperationInfo op = inf.getOperation("foo");
		assertThat(op).as("foo operation not exposed").isNotNull();
		assertThat(op.getDescription()).isEqualTo("invoke foo");

		assertThat(inf.getOperation("doNotExpose")).as("doNotExpose operation should not be exposed").isNull();

		ModelMBeanAttributeInfo attr = inf.getAttribute("Bar");
		assertThat(attr).as("bar attribute not exposed").isNotNull();
		assertThat(attr.getDescription()).isEqualTo("Bar description");

		ModelMBeanAttributeInfo attr2 = inf.getAttribute("CacheEntries");
		assertThat(attr2).as("cacheEntries attribute not exposed").isNotNull();
		assertThat(attr2.getDescriptor().getFieldValue("metricType")).as("Metric Type should be COUNTER").isEqualTo("COUNTER");
	}


	@Override
	protected JmxAttributeSource getAttributeSource() {
		return new AnnotationJmxAttributeSource();
	}

	@Override
	protected String getObjectName() {
		return OBJECT_NAME;
	}

	@Override
	protected ITestBean createJmxTestBean() {
		return new AnnotationTestSubBean();
	}

	@Override
	protected String getApplicationContextPath() {
		return "org/springframework/jmx/export/annotation/annotations.xml";
	}

	@Override
	protected int getExpectedAttributeCount() {
		return super.getExpectedAttributeCount() + 1;
	}

	@Override
	protected int getExpectedOperationCount() {
		return super.getExpectedOperationCount() + 4;
	}

}
