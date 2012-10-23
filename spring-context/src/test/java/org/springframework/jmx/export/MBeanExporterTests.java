/*
 * Copyright 2002-2011 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.Attribute;
import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.modelmbean.ModelMBeanInfo;

import org.junit.Ignore;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.TestBean;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jmx.AbstractMBeanServerTests;
import org.springframework.jmx.IJmxTestBean;
import org.springframework.jmx.JmxTestBean;
import org.springframework.jmx.export.assembler.AutodetectCapableMBeanInfoAssembler;
import org.springframework.jmx.export.assembler.MBeanInfoAssembler;
import org.springframework.jmx.export.assembler.SimpleReflectiveMBeanInfoAssembler;
import org.springframework.jmx.export.naming.SelfNaming;
import org.springframework.jmx.support.ObjectNameManager;

import test.interceptor.NopInterceptor;

/**
 * Integration tests for the {@link MBeanExporter} class.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Rick Evans
 * @author Mark Fisher
 * @author Chris Beams
 * @author Sam Brannen
 */
@SuppressWarnings("deprecation")
public final class MBeanExporterTests extends AbstractMBeanServerTests {

	private static final String OBJECT_NAME = "spring:test=jmxMBeanAdaptor";

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void testRegisterNonNotificationListenerType() throws Exception {
		Map listeners = new HashMap();
		// put a non-NotificationListener instance in as a value...
		listeners.put("*", this);
		MBeanExporter exporter = new MBeanExporter();
		try {
			exporter.setNotificationListenerMappings(listeners);
			fail("Must have thrown a ClassCastException when registering a non-NotificationListener instance as a NotificationListener.");
		}
		catch (ClassCastException expected) {
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void testRegisterNullNotificationListenerType() throws Exception {
		Map listeners = new HashMap();
		// put null in as a value...
		listeners.put("*", null);
		MBeanExporter exporter = new MBeanExporter();
		try {
			exporter.setNotificationListenerMappings(listeners);
			fail("Must have thrown an IllegalArgumentException when registering a null instance as a NotificationListener.");
		}
		catch (IllegalArgumentException expected) {
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void testRegisterNotificationListenerForNonExistentMBean() throws Exception {
		Map listeners = new HashMap();
		NotificationListener dummyListener = new NotificationListener() {
			public void handleNotification(Notification notification, Object handback) {
				throw new UnsupportedOperationException();
			}
		};
		// the MBean with the supplied object name does not exist...
		listeners.put("spring:type=Test", dummyListener);
		MBeanExporter exporter = new MBeanExporter();
		exporter.setBeans(getBeanMap());
		exporter.setServer(server);
		exporter.setNotificationListenerMappings(listeners);
		try {
			exporter.afterPropertiesSet();
			fail("Must have thrown an MBeanExportException when registering a NotificationListener on a non-existent MBean.");
		}
		catch (MBeanExportException expected) {
			assertTrue(expected.contains(InstanceNotFoundException.class));
		}
	}

	public void testWithSuppliedMBeanServer() throws Exception {
		MBeanExporter exporter = new MBeanExporter();
		exporter.setBeans(getBeanMap());
		exporter.setServer(server);
		exporter.afterPropertiesSet();
		assertIsRegistered("The bean was not registered with the MBeanServer",
				ObjectNameManager.getInstance(OBJECT_NAME));
	}

	/** Fails if JVM platform MBean server has been started already
	public void testWithLocatedMBeanServer() throws Exception {
		MBeanExporter adaptor = new MBeanExporter();
		adaptor.setBeans(getBeanMap());
		adaptor.afterPropertiesSet();
		assertIsRegistered("The bean was not registered with the MBeanServer", ObjectNameManager.getInstance(OBJECT_NAME));
		server.unregisterMBean(new ObjectName(OBJECT_NAME));
	}
	*/

	public void testUserCreatedMBeanRegWithDynamicMBean() throws Exception {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("spring:name=dynBean", new TestDynamicMBean());

		InvokeDetectAssembler asm = new InvokeDetectAssembler();

		MBeanExporter exporter = new MBeanExporter();
		exporter.setServer(server);
		exporter.setBeans(map);
		exporter.setAssembler(asm);
		exporter.afterPropertiesSet();

		Object name = server.getAttribute(ObjectNameManager.getInstance("spring:name=dynBean"), "Name");
		assertEquals("The name attribute is incorrect", "Rob Harrop", name);
		assertFalse("Assembler should not have been invoked", asm.invoked);
	}

	public void testAutodetectMBeans() throws Exception {
		XmlBeanFactory bf = new XmlBeanFactory(new ClassPathResource("autodetectMBeans.xml", getClass()));
		try {
			bf.getBean("exporter");
			MBeanServer server = (MBeanServer) bf.getBean("server");
			ObjectInstance instance = server.getObjectInstance(ObjectNameManager.getInstance("spring:mbean=true"));
			assertNotNull(instance);
			instance = server.getObjectInstance(ObjectNameManager.getInstance("spring:mbean2=true"));
			assertNotNull(instance);
			instance = server.getObjectInstance(ObjectNameManager.getInstance("spring:mbean3=true"));
			assertNotNull(instance);
		} finally {
			bf.destroySingletons();
		}
	}

	public void testAutodetectWithExclude() throws Exception {
		XmlBeanFactory bf = new XmlBeanFactory(new ClassPathResource("autodetectMBeans.xml", getClass()));
		try {
			bf.getBean("exporter");
			MBeanServer server = (MBeanServer) bf.getBean("server");
			ObjectInstance instance = server.getObjectInstance(ObjectNameManager.getInstance("spring:mbean=true"));
			assertNotNull(instance);

			try {
				server.getObjectInstance(ObjectNameManager.getInstance("spring:mbean=false"));
				fail("MBean with name spring:mbean=false should have been excluded");
			} catch (InstanceNotFoundException expected) {
			}
		} finally {
			bf.destroySingletons();
		}
	}

	public void testAutodetectLazyMBeans() throws Exception {
		XmlBeanFactory bf = new XmlBeanFactory(new ClassPathResource("autodetectLazyMBeans.xml", getClass()));
		try {
			bf.getBean("exporter");
			MBeanServer server = (MBeanServer) bf.getBean("server");

			ObjectName oname = ObjectNameManager.getInstance("spring:mbean=true");
			assertNotNull(server.getObjectInstance(oname));
			String name = (String) server.getAttribute(oname, "Name");
			assertEquals("Invalid name returned", "Rob Harrop", name);

			oname = ObjectNameManager.getInstance("spring:mbean=another");
			assertNotNull(server.getObjectInstance(oname));
			name = (String) server.getAttribute(oname, "Name");
			assertEquals("Invalid name returned", "Juergen Hoeller", name);
		} finally {
			bf.destroySingletons();
		}
	}

	public void testAutodetectNoMBeans() throws Exception {
		XmlBeanFactory bf = new XmlBeanFactory(new ClassPathResource("autodetectNoMBeans.xml", getClass()));
		try {
			bf.getBean("exporter");
		} finally {
			bf.destroySingletons();
		}
	}

	public void testWithMBeanExporterListeners() throws Exception {
		MockMBeanExporterListener listener1 = new MockMBeanExporterListener();
		MockMBeanExporterListener listener2 = new MockMBeanExporterListener();

		MBeanExporter exporter = new MBeanExporter();
		exporter.setBeans(getBeanMap());
		exporter.setServer(server);
		exporter.setListeners(new MBeanExporterListener[] { listener1, listener2 });
		exporter.afterPropertiesSet();
		exporter.destroy();

		assertListener(listener1);
		assertListener(listener2);
	}

	public void testExportJdkProxy() throws Exception {
		JmxTestBean bean = new JmxTestBean();
		bean.setName("Rob Harrop");

		ProxyFactory factory = new ProxyFactory();
		factory.setTarget(bean);
		factory.addAdvice(new NopInterceptor());
		factory.setInterfaces(new Class[] { IJmxTestBean.class });

		IJmxTestBean proxy = (IJmxTestBean) factory.getProxy();
		String name = "bean:mmm=whatever";

		Map<String, Object> beans = new HashMap<String, Object>();
		beans.put(name, proxy);

		MBeanExporter exporter = new MBeanExporter();
		exporter.setServer(server);
		exporter.setBeans(beans);
		exporter.registerBeans();

		ObjectName oname = ObjectName.getInstance(name);
		Object nameValue = server.getAttribute(oname, "Name");
		assertEquals("Rob Harrop", nameValue);
	}

	public void testSelfNaming() throws Exception {
		ObjectName objectName = ObjectNameManager.getInstance(OBJECT_NAME);
		SelfNamingTestBean testBean = new SelfNamingTestBean();
		testBean.setObjectName(objectName);

		Map<String, Object> beans = new HashMap<String, Object>();
		beans.put("foo", testBean);

		MBeanExporter exporter = new MBeanExporter();
		exporter.setServer(server);
		exporter.setBeans(beans);

		exporter.afterPropertiesSet();

		ObjectInstance instance = server.getObjectInstance(objectName);
		assertNotNull(instance);
	}

	public void testRegisterIgnoreExisting() throws Exception {
		ObjectName objectName = ObjectNameManager.getInstance(OBJECT_NAME);

		Person preRegistered = new Person();
		preRegistered.setName("Rob Harrop");

		server.registerMBean(preRegistered, objectName);

		Person springRegistered = new Person();
		springRegistered.setName("Sally Greenwood");

		String objectName2 = "spring:test=equalBean";

		Map<String, Object> beans = new HashMap<String, Object>();
		beans.put(objectName.toString(), springRegistered);
		beans.put(objectName2, springRegistered);

		MBeanExporter exporter = new MBeanExporter();
		exporter.setServer(server);
		exporter.setBeans(beans);
		exporter.setRegistrationBehavior(MBeanExporter.REGISTRATION_IGNORE_EXISTING);

		exporter.afterPropertiesSet();

		ObjectInstance instance = server.getObjectInstance(objectName);
		assertNotNull(instance);
		ObjectInstance instance2 = server.getObjectInstance(new ObjectName(objectName2));
		assertNotNull(instance2);

		// should still be the first bean with name Rob Harrop
		assertEquals("Rob Harrop", server.getAttribute(objectName, "Name"));
	}

	public void testRegisterReplaceExisting() throws Exception {
		ObjectName objectName = ObjectNameManager.getInstance(OBJECT_NAME);

		Person preRegistered = new Person();
		preRegistered.setName("Rob Harrop");

		server.registerMBean(preRegistered, objectName);

		Person springRegistered = new Person();
		springRegistered.setName("Sally Greenwood");

		Map<String, Object> beans = new HashMap<String, Object>();
		beans.put(objectName.toString(), springRegistered);

		MBeanExporter exporter = new MBeanExporter();
		exporter.setServer(server);
		exporter.setBeans(beans);
		exporter.setRegistrationBehavior(MBeanExporter.REGISTRATION_REPLACE_EXISTING);

		exporter.afterPropertiesSet();

		ObjectInstance instance = server.getObjectInstance(objectName);
		assertNotNull(instance);

		// should still be the new bean with name Sally Greenwood
		assertEquals("Sally Greenwood", server.getAttribute(objectName, "Name"));
	}

	public void testWithExposeClassLoader() throws Exception {
		String name = "Rob Harrop";
		String otherName = "Juergen Hoeller";

		JmxTestBean bean = new JmxTestBean();
		bean.setName(name);
		ObjectName objectName = ObjectNameManager.getInstance("spring:type=Test");

		Map<String, Object> beans = new HashMap<String, Object>();
		beans.put(objectName.toString(), bean);

		MBeanExporter exporter = new MBeanExporter();
		exporter.setServer(getServer());
		exporter.setBeans(beans);
		exporter.setExposeManagedResourceClassLoader(true);
		exporter.afterPropertiesSet();

		assertIsRegistered("Bean instance not registered", objectName);

		Object result = server.invoke(objectName, "add", new Object[] { new Integer(2), new Integer(3) }, new String[] {
				int.class.getName(), int.class.getName() });

		assertEquals("Incorrect result return from add", result, new Integer(5));
		assertEquals("Incorrect attribute value", name, server.getAttribute(objectName, "Name"));

		server.setAttribute(objectName, new Attribute("Name", otherName));
		assertEquals("Incorrect updated name.", otherName, bean.getName());
	}

	public void testBonaFideMBeanIsNotExportedWhenAutodetectIsTotallyTurnedOff() throws Exception {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(Person.class);
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		factory.registerBeanDefinition("^&_invalidObjectName_(*", builder.getBeanDefinition());
		String exportedBeanName = "export.me.please";
		factory.registerSingleton(exportedBeanName, new TestBean());

		MBeanExporter exporter = new MBeanExporter();
		Map<String, Object> beansToExport = new HashMap<String, Object>();
		beansToExport.put(OBJECT_NAME, exportedBeanName);
		exporter.setBeans(beansToExport);
		exporter.setServer(getServer());
		exporter.setBeanFactory(factory);
		exporter.setAutodetectMode(MBeanExporter.AUTODETECT_NONE);
		// MBean has a bad ObjectName, so if said MBean is autodetected, an exception will be thrown...
		exporter.afterPropertiesSet();
	}

	public void testOnlyBonaFideMBeanIsExportedWhenAutodetectIsMBeanOnly() throws Exception {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(Person.class);
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		factory.registerBeanDefinition(OBJECT_NAME, builder.getBeanDefinition());
		String exportedBeanName = "spring:type=TestBean";
		factory.registerSingleton(exportedBeanName, new TestBean());

		MBeanExporter exporter = new MBeanExporter();
		exporter.setServer(getServer());
		exporter.setAssembler(new NamedBeanAutodetectCapableMBeanInfoAssemblerStub(exportedBeanName));
		exporter.setBeanFactory(factory);
		exporter.setAutodetectMode(MBeanExporter.AUTODETECT_MBEAN);
		exporter.afterPropertiesSet();
		assertIsRegistered("Bona fide MBean not autodetected in AUTODETECT_MBEAN mode",
				ObjectNameManager.getInstance(OBJECT_NAME));
		assertIsNotRegistered("Bean autodetected and (only) AUTODETECT_MBEAN mode is on",
				ObjectNameManager.getInstance(exportedBeanName));
	}

	public void testBonaFideMBeanAndRegularBeanExporterWithAutodetectAll() throws Exception {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(Person.class);
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		factory.registerBeanDefinition(OBJECT_NAME, builder.getBeanDefinition());
		String exportedBeanName = "spring:type=TestBean";
		factory.registerSingleton(exportedBeanName, new TestBean());
		String notToBeExportedBeanName = "spring:type=NotToBeExported";
		factory.registerSingleton(notToBeExportedBeanName, new TestBean());

		MBeanExporter exporter = new MBeanExporter();
		exporter.setServer(getServer());
		exporter.setAssembler(new NamedBeanAutodetectCapableMBeanInfoAssemblerStub(exportedBeanName));
		exporter.setBeanFactory(factory);
		exporter.setAutodetectMode(MBeanExporter.AUTODETECT_ALL);
		exporter.afterPropertiesSet();
		assertIsRegistered("Bona fide MBean not autodetected in (AUTODETECT_ALL) mode",
				ObjectNameManager.getInstance(OBJECT_NAME));
		assertIsRegistered("Bean not autodetected in (AUTODETECT_ALL) mode",
				ObjectNameManager.getInstance(exportedBeanName));
		assertIsNotRegistered("Bean autodetected and did not satisfy the autodetect info assembler",
				ObjectNameManager.getInstance(notToBeExportedBeanName));
	}

	public void testBonaFideMBeanIsNotExportedWithAutodetectAssembler() throws Exception {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(Person.class);
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		factory.registerBeanDefinition(OBJECT_NAME, builder.getBeanDefinition());
		String exportedBeanName = "spring:type=TestBean";
		factory.registerSingleton(exportedBeanName, new TestBean());

		MBeanExporter exporter = new MBeanExporter();
		exporter.setServer(getServer());
		exporter.setAssembler(new NamedBeanAutodetectCapableMBeanInfoAssemblerStub(exportedBeanName));
		exporter.setBeanFactory(factory);
		exporter.setAutodetectMode(MBeanExporter.AUTODETECT_ASSEMBLER);
		exporter.afterPropertiesSet();
		assertIsNotRegistered("Bona fide MBean was autodetected in AUTODETECT_ASSEMBLER mode - must not have been",
				ObjectNameManager.getInstance(OBJECT_NAME));
		assertIsRegistered("Bean not autodetected in AUTODETECT_ASSEMBLER mode",
				ObjectNameManager.getInstance(exportedBeanName));
	}

	/**
	 * Want to ensure that said MBean is not exported twice.
	 */
	public void testBonaFideMBeanExplicitlyExportedAndAutodetectionIsOn() throws Exception {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(Person.class);
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		factory.registerBeanDefinition(OBJECT_NAME, builder.getBeanDefinition());

		MBeanExporter exporter = new MBeanExporter();
		exporter.setServer(getServer());
		Map<String, Object> beansToExport = new HashMap<String, Object>();
		beansToExport.put(OBJECT_NAME, OBJECT_NAME);
		exporter.setBeans(beansToExport);
		exporter.setAssembler(new NamedBeanAutodetectCapableMBeanInfoAssemblerStub(OBJECT_NAME));
		exporter.setBeanFactory(factory);
		exporter.setAutodetectMode(MBeanExporter.AUTODETECT_ASSEMBLER);
		exporter.afterPropertiesSet();
		assertIsRegistered("Explicitly exported bona fide MBean obviously not exported.",
				ObjectNameManager.getInstance(OBJECT_NAME));
	}

	public void testSetAutodetectModeToOutOfRangeNegativeValue() throws Exception {
		try {
			MBeanExporter exporter = new MBeanExporter();
			exporter.setAutodetectMode(-1);
			fail("Must have failed when supplying an invalid negative out-of-range autodetect mode");
		}
		catch (IllegalArgumentException expected) {
		}
	}

	public void testSetAutodetectModeToOutOfRangePositiveValue() throws Exception {
		try {
			MBeanExporter exporter = new MBeanExporter();
			exporter.setAutodetectMode(5);
			fail("Must have failed when supplying an invalid positive out-of-range autodetect mode");
		}
		catch (IllegalArgumentException expected) {
		}
	}

	public void testSetAutodetectModeNameToNull() throws Exception {
		try {
			MBeanExporter exporter = new MBeanExporter();
			exporter.setAutodetectModeName(null);
			fail("Must have failed when supplying a null autodetect mode name");
		}
		catch (IllegalArgumentException expected) {
		}
	}

	public void testSetAutodetectModeNameToAnEmptyString() throws Exception {
		try {
			MBeanExporter exporter = new MBeanExporter();
			exporter.setAutodetectModeName("");
			fail("Must have failed when supplying an empty autodetect mode name");
		}
		catch (IllegalArgumentException expected) {
		}
	}

	public void testSetAutodetectModeNameToAWhitespacedString() throws Exception {
		try {
			MBeanExporter exporter = new MBeanExporter();
			exporter.setAutodetectModeName("  \t");
			fail("Must have failed when supplying a whitespace-only autodetect mode name");
		}
		catch (IllegalArgumentException expected) {
		}
	}

	public void testSetAutodetectModeNameToARubbishValue() throws Exception {
		try {
			MBeanExporter exporter = new MBeanExporter();
			exporter.setAutodetectModeName("That Hansel is... *sssooo* hot right now!");
			fail("Must have failed when supplying a whitespace-only autodetect mode name");
		}
		catch (IllegalArgumentException expected) {
		}
	}

	public void testNotRunningInBeanFactoryAndPassedBeanNameToExport() throws Exception {
		try {
			MBeanExporter exporter = new MBeanExporter();
			Map<String, Object> beans = new HashMap<String, Object>();
			beans.put(OBJECT_NAME, "beanName");
			exporter.setBeans(beans);
			exporter.afterPropertiesSet();
			fail("Expecting exception because MBeanExporter is not running in a BeanFactory and was passed bean name to (lookup and then) export");
		}
		catch (MBeanExportException expected) {
		}
	}

	public void testNotRunningInBeanFactoryAndAutodetectionIsOn() throws Exception {
		try {
			MBeanExporter exporter = new MBeanExporter();
			exporter.setAutodetectMode(MBeanExporter.AUTODETECT_ALL);
			exporter.afterPropertiesSet();
			fail("Expecting exception because MBeanExporter is not running in a BeanFactory and was configured to autodetect beans");
		}
		catch (MBeanExportException expected) {
		}
	}

	/**
	 * SPR-2158
	 */
	public void testMBeanIsNotUnregisteredSpuriouslyIfSomeExternalProcessHasUnregisteredMBean() throws Exception {
		MBeanExporter exporter = new MBeanExporter();
		exporter.setBeans(getBeanMap());
		exporter.setServer(this.server);
		MockMBeanExporterListener listener = new MockMBeanExporterListener();
		exporter.setListeners(new MBeanExporterListener[] { listener });
		exporter.afterPropertiesSet();
		assertIsRegistered("The bean was not registered with the MBeanServer",
				ObjectNameManager.getInstance(OBJECT_NAME));

		this.server.unregisterMBean(new ObjectName(OBJECT_NAME));
		exporter.destroy();
		assertEquals("Listener should not have been invoked (MBean previously unregistered by external agent)", 0,
				listener.getUnregistered().size());
	}

	/**
	 * SPR-3302
	 */
	public void testBeanNameCanBeUsedInNotificationListenersMap() throws Exception {
		String beanName = "charlesDexterWard";
		BeanDefinitionBuilder testBean = BeanDefinitionBuilder.rootBeanDefinition(JmxTestBean.class);

		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		factory.registerBeanDefinition(beanName, testBean.getBeanDefinition());
		factory.preInstantiateSingletons();
		Object testBeanInstance = factory.getBean(beanName);

		MBeanExporter exporter = new MBeanExporter();
		exporter.setServer(getServer());
		Map<String, Object> beansToExport = new HashMap<String, Object>();
		beansToExport.put("test:what=ever", testBeanInstance);
		exporter.setBeans(beansToExport);
		exporter.setBeanFactory(factory);
		StubNotificationListener listener = new StubNotificationListener();
		exporter.setNotificationListenerMappings(Collections.singletonMap(beanName, listener));

		exporter.afterPropertiesSet();
	}

	public void testWildcardCanBeUsedInNotificationListenersMap() throws Exception {
		String beanName = "charlesDexterWard";
		BeanDefinitionBuilder testBean = BeanDefinitionBuilder.rootBeanDefinition(JmxTestBean.class);

		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		factory.registerBeanDefinition(beanName, testBean.getBeanDefinition());
		factory.preInstantiateSingletons();
		Object testBeanInstance = factory.getBean(beanName);

		MBeanExporter exporter = new MBeanExporter();
		exporter.setServer(getServer());
		Map<String, Object> beansToExport = new HashMap<String, Object>();
		beansToExport.put("test:what=ever", testBeanInstance);
		exporter.setBeans(beansToExport);
		exporter.setBeanFactory(factory);
		StubNotificationListener listener = new StubNotificationListener();
		exporter.setNotificationListenerMappings(Collections.singletonMap("*", listener));

		exporter.afterPropertiesSet();
	}

	/*
	 * SPR-3625
	 */
	public void testMBeanIsUnregisteredForRuntimeExceptionDuringInitialization() throws Exception {
		BeanDefinitionBuilder builder1 = BeanDefinitionBuilder.rootBeanDefinition(Person.class);
		BeanDefinitionBuilder builder2 = BeanDefinitionBuilder
				.rootBeanDefinition(RuntimeExceptionThrowingConstructorBean.class);

		String objectName1 = "spring:test=bean1";
		String objectName2 = "spring:test=bean2";

		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		factory.registerBeanDefinition(objectName1, builder1.getBeanDefinition());
		factory.registerBeanDefinition(objectName2, builder2.getBeanDefinition());

		MBeanExporter exporter = new MBeanExporter();
		exporter.setServer(getServer());
		Map<String, Object> beansToExport = new HashMap<String, Object>();
		beansToExport.put(objectName1, objectName1);
		beansToExport.put(objectName2, objectName2);
		exporter.setBeans(beansToExport);
		exporter.setBeanFactory(factory);

		try {
			exporter.afterPropertiesSet();
			fail("Must have failed during creation of RuntimeExceptionThrowingConstructorBean");
		}
		catch (RuntimeException expected) {
		}

		assertIsNotRegistered("Must have unregistered all previously registered MBeans due to RuntimeException",
				ObjectNameManager.getInstance(objectName1));
		assertIsNotRegistered("Must have never registered this MBean due to RuntimeException",
				ObjectNameManager.getInstance(objectName2));
	}

	private Map<String, Object> getBeanMap() {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put(OBJECT_NAME, new JmxTestBean());
		return map;
	}

	private void assertListener(MockMBeanExporterListener listener) throws MalformedObjectNameException {
		ObjectName desired = ObjectNameManager.getInstance(OBJECT_NAME);
		assertEquals("Incorrect number of registrations", 1, listener.getRegistered().size());
		assertEquals("Incorrect number of unregistrations", 1, listener.getUnregistered().size());
		assertEquals("Incorrect ObjectName in register", desired, listener.getRegistered().get(0));
		assertEquals("Incorrect ObjectName in unregister", desired, listener.getUnregistered().get(0));
	}

	private static class InvokeDetectAssembler implements MBeanInfoAssembler {

		private boolean invoked = false;

		public ModelMBeanInfo getMBeanInfo(Object managedResource, String beanKey) throws JMException {
			invoked = true;
			return null;
		}
	}

	private static class MockMBeanExporterListener implements MBeanExporterListener {

		private List<ObjectName> registered = new ArrayList<ObjectName>();

		private List<ObjectName> unregistered = new ArrayList<ObjectName>();

		public void mbeanRegistered(ObjectName objectName) {
			registered.add(objectName);
		}

		public void mbeanUnregistered(ObjectName objectName) {
			unregistered.add(objectName);
		}

		public List<ObjectName> getRegistered() {
			return registered;
		}

		public List<ObjectName> getUnregistered() {
			return unregistered;
		}
	}

	private static class SelfNamingTestBean implements SelfNaming {

		private ObjectName objectName;

		public void setObjectName(ObjectName objectName) {
			this.objectName = objectName;
		}

		public ObjectName getObjectName() throws MalformedObjectNameException {
			return this.objectName;
		}
	}

	public static interface PersonMBean {

		String getName();
	}

	public static class Person implements PersonMBean {

		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	public static final class StubNotificationListener implements NotificationListener {

		private List<Notification> notifications = new ArrayList<Notification>();

		public void handleNotification(Notification notification, Object handback) {
			this.notifications.add(notification);
		}

		public List<Notification> getNotifications() {
			return this.notifications;
		}
	}

	private static class RuntimeExceptionThrowingConstructorBean {

		@SuppressWarnings("unused")
		public RuntimeExceptionThrowingConstructorBean() {
			throw new RuntimeException();
		}
	}

	private static final class NamedBeanAutodetectCapableMBeanInfoAssemblerStub extends
			SimpleReflectiveMBeanInfoAssembler implements AutodetectCapableMBeanInfoAssembler {

		private String namedBean;

		public NamedBeanAutodetectCapableMBeanInfoAssemblerStub(String namedBean) {
			this.namedBean = namedBean;
		}

		public boolean includeBean(Class<?> beanClass, String beanName) {
			return this.namedBean.equals(beanName);
		}
	}

}
