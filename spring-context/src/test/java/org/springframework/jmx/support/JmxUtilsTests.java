/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.jmx.support;

import java.beans.PropertyDescriptor;

import javax.management.DynamicMBean;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import junit.framework.TestCase;

import org.springframework.beans.BeanWrapperImpl;
import org.springframework.core.JdkVersion;
import org.springframework.jmx.IJmxTestBean;
import org.springframework.jmx.JmxTestBean;
import org.springframework.jmx.export.TestDynamicMBean;
import org.springframework.util.ObjectUtils;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
public class JmxUtilsTests extends TestCase {

	public void testIsMBeanWithDynamicMBean() throws Exception {
		DynamicMBean mbean = new TestDynamicMBean();
		assertTrue("Dynamic MBean not detected correctly", JmxUtils.isMBean(mbean.getClass()));
	}

	public void testIsMBeanWithStandardMBeanWrapper() throws Exception {
		StandardMBean mbean = new StandardMBean(new JmxTestBean(), IJmxTestBean.class);
		assertTrue("Standard MBean not detected correctly", JmxUtils.isMBean(mbean.getClass()));
	}

	public void testIsMBeanWithStandardMBeanInherited() throws Exception {
		StandardMBean mbean = new StandardMBeanImpl();
		assertTrue("Standard MBean not detected correctly", JmxUtils.isMBean(mbean.getClass()));
	}

	public void testNotAnMBean() throws Exception {
		assertFalse("Object incorrectly identified as an MBean", JmxUtils.isMBean(Object.class));
	}

	public void testSimpleMBean() throws Exception {
		Foo foo = new Foo();
		assertTrue("Simple MBean not detected correctly", JmxUtils.isMBean(foo.getClass()));
	}

	public void testSimpleMXBean() throws Exception {
		FooX foo = new FooX();
		assertTrue("Simple MXBean not detected correctly", JmxUtils.isMBean(foo.getClass()));
	}

	public void testSimpleMBeanThroughInheritance() throws Exception {
		Bar bar = new Bar();
		Abc abc = new Abc();
		assertTrue("Simple MBean (through inheritance) not detected correctly",
				JmxUtils.isMBean(bar.getClass()));
		assertTrue("Simple MBean (through 2 levels of inheritance) not detected correctly",
				JmxUtils.isMBean(abc.getClass()));
	}

	public void testGetAttributeNameWithStrictCasing() {
		PropertyDescriptor pd = new BeanWrapperImpl(AttributeTestBean.class).getPropertyDescriptor("name");
		String attributeName = JmxUtils.getAttributeName(pd, true);
		assertEquals("Incorrect casing on attribute name", "Name", attributeName);
	}

	public void testGetAttributeNameWithoutStrictCasing() {
		PropertyDescriptor pd = new BeanWrapperImpl(AttributeTestBean.class).getPropertyDescriptor("name");
		String attributeName = JmxUtils.getAttributeName(pd, false);
		assertEquals("Incorrect casing on attribute name", "name", attributeName);
	}

	public void testAppendIdentityToObjectName() throws MalformedObjectNameException {
		ObjectName objectName = ObjectNameManager.getInstance("spring:type=Test");
		Object managedResource = new Object();
		ObjectName uniqueName = JmxUtils.appendIdentityToObjectName(objectName, managedResource);

		String typeProperty = "type";

		assertEquals("Domain of transformed name is incorrect", objectName.getDomain(), uniqueName.getDomain());
		assertEquals("Type key is incorrect", objectName.getKeyProperty(typeProperty), uniqueName.getKeyProperty("type"));
		assertEquals("Identity key is incorrect", ObjectUtils.getIdentityHexString(managedResource), uniqueName.getKeyProperty(JmxUtils.IDENTITY_OBJECT_NAME_KEY));
	}

	public void testLocatePlatformMBeanServer() {
		if(JdkVersion.getMajorJavaVersion() < JdkVersion.JAVA_15) {
			return;
		}

		MBeanServer server = null;
		try {
			server = JmxUtils.locateMBeanServer();
		}
		finally {
			if (server != null) {
				MBeanServerFactory.releaseMBeanServer(server);
			}
		}
	}

	public void testIsMBean() {
		// Correctly returns true for a class
		assertTrue(JmxUtils.isMBean(JmxClass.class));

		// Correctly returns false since JmxUtils won't navigate to the extended interface
		assertFalse(JmxUtils.isMBean(SpecializedJmxInterface.class));

		// Incorrectly returns true since it doesn't detect that this is an interface
		assertFalse(JmxUtils.isMBean(JmxInterface.class));
	}


	public static class AttributeTestBean {

		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}


	public static class StandardMBeanImpl extends StandardMBean implements IJmxTestBean {

		public StandardMBeanImpl() throws NotCompliantMBeanException {
			super(IJmxTestBean.class);
		}

		@Override
		public int add(int x, int y) {
			return 0;
		}

		@Override
		public long myOperation() {
			return 0;
		}

		@Override
		public int getAge() {
			return 0;
		}

		@Override
		public void setAge(int age) {
		}

		@Override
		public void setName(String name) {
		}

		@Override
		public String getName() {
			return null;
		}

		@Override
		public void dontExposeMe() {
		}
	}


	public static interface FooMBean {

		String getName();
	}


	public static class Foo implements FooMBean {

		@Override
		public String getName() {
			return "Rob Harrop";
		}
	}


	public static interface FooMXBean {

		String getName();
	}


	public static class FooX implements FooMXBean {

		@Override
		public String getName() {
			return "Rob Harrop";
		}
	}


	public static class Bar extends Foo {

	}


	public static class Abc extends Bar {

	}


	private static interface JmxInterfaceMBean {

	}


	private static interface JmxInterface extends JmxInterfaceMBean {

	}


	private static interface SpecializedJmxInterface extends JmxInterface {

	}


	private static interface JmxClassMBean {

	}


	private static class JmxClass implements JmxClassMBean {

	}

}
