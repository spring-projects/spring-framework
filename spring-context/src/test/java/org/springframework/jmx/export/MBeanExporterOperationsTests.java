/*
 * Copyright 2002-present the original author or authors.
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

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanInfo;
import javax.management.ObjectName;
import javax.management.modelmbean.ModelMBeanInfo;
import javax.management.modelmbean.ModelMBeanInfoSupport;
import javax.management.modelmbean.RequiredModelMBean;

import org.junit.jupiter.api.Test;

import org.springframework.jmx.AbstractMBeanServerTests;
import org.springframework.jmx.JmxTestBean;
import org.springframework.jmx.support.ObjectNameManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
class MBeanExporterOperationsTests extends AbstractMBeanServerTests {

	@Test
	void testRegisterManagedResourceWithUserSuppliedObjectName() throws Exception {
		ObjectName objectName = ObjectNameManager.getInstance("spring:name=Foo");

		JmxTestBean bean = new JmxTestBean();
		bean.setName("Rob Harrop");

		MBeanExporter exporter = new MBeanExporter();
		exporter.setServer(getServer());
		exporter.registerManagedResource(bean, objectName);

		String name = (String) getServer().getAttribute(objectName, "Name");
		assertThat(bean.getName()).as("Incorrect name on MBean").isEqualTo(name);
	}

	@Test
	void testRegisterExistingMBeanWithUserSuppliedObjectName() throws Exception {
		ObjectName objectName = ObjectNameManager.getInstance("spring:name=Foo");
		ModelMBeanInfo info = new ModelMBeanInfoSupport("myClass", "myDescription", null, null, null, null);
		RequiredModelMBean bean = new RequiredModelMBean(info);

		MBeanExporter exporter = new MBeanExporter();
		exporter.setServer(getServer());
		exporter.registerManagedResource(bean, objectName);

		MBeanInfo infoFromServer = getServer().getMBeanInfo(objectName);
		assertThat(infoFromServer).isEqualTo(info);
	}

	@Test
	void testRegisterManagedResourceWithGeneratedObjectName() throws Exception {
		final ObjectName objectNameTemplate = ObjectNameManager.getInstance("spring:type=Test");

		MBeanExporter exporter = new MBeanExporter();
		exporter.setServer(getServer());
		exporter.setNamingStrategy((managedBean, beanKey) -> objectNameTemplate);

		JmxTestBean bean1 = new JmxTestBean();
		JmxTestBean bean2 = new JmxTestBean();

		ObjectName reg1 = exporter.registerManagedResource(bean1);
		ObjectName reg2 = exporter.registerManagedResource(bean2);

		assertIsRegistered("Bean 1 not registered with MBeanServer", reg1);
		assertIsRegistered("Bean 2 not registered with MBeanServer", reg2);

		assertObjectNameMatchesTemplate(objectNameTemplate, reg1);
		assertObjectNameMatchesTemplate(objectNameTemplate, reg2);
	}

	@Test
	void testRegisterManagedResourceWithGeneratedObjectNameWithoutUniqueness() throws Exception {
		final ObjectName objectNameTemplate = ObjectNameManager.getInstance("spring:type=Test");

		MBeanExporter exporter = new MBeanExporter();
		exporter.setServer(getServer());
		exporter.setEnsureUniqueRuntimeObjectNames(false);
		exporter.setNamingStrategy((managedBean, beanKey) -> objectNameTemplate);

		JmxTestBean bean1 = new JmxTestBean();
		JmxTestBean bean2 = new JmxTestBean();

		ObjectName reg1 = exporter.registerManagedResource(bean1);
		assertIsRegistered("Bean 1 not registered with MBeanServer", reg1);

		assertThatExceptionOfType(MBeanExportException.class).isThrownBy(()->
				exporter.registerManagedResource(bean2))
			.withCauseExactlyInstanceOf(InstanceAlreadyExistsException.class);
	}

	private void assertObjectNameMatchesTemplate(ObjectName objectNameTemplate, ObjectName registeredName) {
		assertThat(registeredName.getDomain()).as("Domain is incorrect").isEqualTo(objectNameTemplate.getDomain());
	}

}
