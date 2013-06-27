/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.jmx.export;

import javax.management.InstanceAlreadyExistsException;
import javax.management.ObjectName;
import javax.management.MBeanInfo;
import javax.management.modelmbean.RequiredModelMBean;
import javax.management.modelmbean.ModelMBeanInfo;
import javax.management.modelmbean.ModelMBeanInfoSupport;

import org.junit.Test;

import org.springframework.jmx.AbstractMBeanServerTests;
import org.springframework.jmx.JmxTestBean;
import org.springframework.jmx.export.naming.ObjectNamingStrategy;
import org.springframework.jmx.support.ObjectNameManager;

import static org.junit.Assert.*;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
public class MBeanExporterOperationsTests extends AbstractMBeanServerTests {

	@Test
	public void testRegisterManagedResourceWithUserSuppliedObjectName() throws Exception {
		ObjectName objectName = ObjectNameManager.getInstance("spring:name=Foo");

		JmxTestBean bean = new JmxTestBean();
		bean.setName("Rob Harrop");

		MBeanExporter exporter = new MBeanExporter();
		exporter.setServer(getServer());
		exporter.registerManagedResource(bean, objectName);

		String name = (String) getServer().getAttribute(objectName, "Name");
		assertEquals("Incorrect name on MBean", name, bean.getName());
	}

	@Test
	public void testRegisterExistingMBeanWithUserSuppliedObjectName() throws Exception {
		ObjectName objectName = ObjectNameManager.getInstance("spring:name=Foo");
		ModelMBeanInfo info = new ModelMBeanInfoSupport("myClass", "myDescription", null, null, null, null);
		RequiredModelMBean bean = new RequiredModelMBean(info);

		MBeanExporter exporter = new MBeanExporter();
		exporter.setServer(getServer());
		exporter.registerManagedResource(bean, objectName);

		MBeanInfo infoFromServer = getServer().getMBeanInfo(objectName);
		assertEquals(info, infoFromServer);
	}

	@Test
	public void testRegisterManagedResourceWithGeneratedObjectName() throws Exception {
		final ObjectName objectNameTemplate = ObjectNameManager.getInstance("spring:type=Test");

		MBeanExporter exporter = new MBeanExporter();
		exporter.setServer(getServer());
		exporter.setNamingStrategy(new ObjectNamingStrategy() {
			@Override
			public ObjectName getObjectName(Object managedBean, String beanKey) {
				return objectNameTemplate;
			}
		});

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
	public void testRegisterManagedResourceWithGeneratedObjectNameWithoutUniqueness() throws Exception {
		final ObjectName objectNameTemplate = ObjectNameManager.getInstance("spring:type=Test");

		MBeanExporter exporter = new MBeanExporter();
		exporter.setServer(getServer());
		exporter.setEnsureUniqueRuntimeObjectNames(false);
		exporter.setNamingStrategy(new ObjectNamingStrategy() {
			@Override
			public ObjectName getObjectName(Object managedBean, String beanKey) {
				return objectNameTemplate;
			}
		});

		JmxTestBean bean1 = new JmxTestBean();
		JmxTestBean bean2 = new JmxTestBean();

		ObjectName reg1 = exporter.registerManagedResource(bean1);
		assertIsRegistered("Bean 1 not registered with MBeanServer", reg1);

		try {
			exporter.registerManagedResource(bean2);
			fail("Shouldn't be able to register a runtime MBean with a reused ObjectName.");
		}
		catch (MBeanExportException e) {
			assertEquals("Incorrect root cause", InstanceAlreadyExistsException.class, e.getCause().getClass());
		}
	}

	private void assertObjectNameMatchesTemplate(ObjectName objectNameTemplate, ObjectName registeredName) {
		assertEquals("Domain is incorrect", objectNameTemplate.getDomain(), registeredName.getDomain());
	}

}
