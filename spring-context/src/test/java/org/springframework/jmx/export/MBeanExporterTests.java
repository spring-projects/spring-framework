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

package org.springframework.jmx.export;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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

import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.testfixture.interceptor.NopInterceptor;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jmx.AbstractMBeanServerTests;
import org.springframework.jmx.IJmxTestBean;
import org.springframework.jmx.JmxTestBean;
import org.springframework.jmx.export.assembler.AutodetectCapableMBeanInfoAssembler;
import org.springframework.jmx.export.assembler.MBeanInfoAssembler;
import org.springframework.jmx.export.assembler.SimpleReflectiveMBeanInfoAssembler;
import org.springframework.jmx.export.naming.SelfNaming;
import org.springframework.jmx.support.ObjectNameManager;
import org.springframework.jmx.support.RegistrationPolicy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Integration tests for the {@link MBeanExporter} class.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Rick Evans
 * @author Mark Fisher
 * @author Chris Beams
 * @author Sam Brannen
 * @author Stephane Nicoll
 */
public class MBeanExporterTests extends AbstractMBeanServerTests {

	private static final String OBJECT_NAME = "spring:test=jmxMBeanAdaptor";


	@Test
	public void testRegisterNullNotificationListenerType() throws Exception {
		Map<String, NotificationListener> listeners = new HashMap<>();
		// put null in as a value...
		listeners.put("*", null);
		MBeanExporter exporter = new MBeanExporter();

		assertThatIllegalArgumentException().isThrownBy(() ->
				exporter.setNotificationListenerMappings(listeners));
	}

	@Test
	public void testRegisterNotificationListenerForNonExistentMBean() throws Exception {
		Map<String, NotificationListener> listeners = new HashMap<>();
		NotificationListener dummyListener = new NotificationListener() {
			@Override
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
		assertThatExceptionOfType(MBeanExportException.class).as("NotificationListener on a non-existent MBean").isThrownBy(() ->
				start(exporter))
			.satisfies(ex -> assertThat(ex.contains(InstanceNotFoundException.class)));
	}

	@Test
	public void testWithSuppliedMBeanServer() throws Exception {
		MBeanExporter exporter = new MBeanExporter();
		exporter.setBeans(getBeanMap());
		exporter.setServer(server);
		try {
			start(exporter);
			assertIsRegistered("The bean was not registered with the MBeanServer",
					ObjectNameManager.getInstance(OBJECT_NAME));
		}
		finally {
			exporter.destroy();
		}
	}

	@Test
	public void testUserCreatedMBeanRegWithDynamicMBean() throws Exception {
		Map<String, Object> map = new HashMap<>();
		map.put("spring:name=dynBean", new TestDynamicMBean());

		InvokeDetectAssembler asm = new InvokeDetectAssembler();

		MBeanExporter exporter = new MBeanExporter();
		exporter.setServer(server);
		exporter.setBeans(map);
		exporter.setAssembler(asm);

		try {
			start(exporter);
			Object name = server.getAttribute(ObjectNameManager.getInstance("spring:name=dynBean"), "Name");
			assertThat(name).as("The name attribute is incorrect").isEqualTo("Rob Harrop");
			assertThat(asm.invoked).as("Assembler should not have been invoked").isFalse();
		}
		finally {
			exporter.destroy();
		}
	}

	@Test
	public void testAutodetectMBeans() throws Exception {
		ConfigurableApplicationContext ctx = load("autodetectMBeans.xml");
		try {
			ctx.getBean("exporter");
			MBeanServer server = ctx.getBean("server", MBeanServer.class);
			ObjectInstance instance = server.getObjectInstance(ObjectNameManager.getInstance("spring:mbean=true"));
			assertThat(instance).isNotNull();
			instance = server.getObjectInstance(ObjectNameManager.getInstance("spring:mbean2=true"));
			assertThat(instance).isNotNull();
			instance = server.getObjectInstance(ObjectNameManager.getInstance("spring:mbean3=true"));
			assertThat(instance).isNotNull();
		}
		finally {
			ctx.close();
		}
	}

	@Test
	public void testAutodetectWithExclude() throws Exception {
		ConfigurableApplicationContext ctx = load("autodetectMBeans.xml");
		try {
			ctx.getBean("exporter");
			MBeanServer server = ctx.getBean("server", MBeanServer.class);
			ObjectInstance instance = server.getObjectInstance(ObjectNameManager.getInstance("spring:mbean=true"));
			assertThat(instance).isNotNull();

			assertThatExceptionOfType(InstanceNotFoundException.class).isThrownBy(() ->
					server.getObjectInstance(ObjectNameManager.getInstance("spring:mbean=false")));
		}
		finally {
			ctx.close();
		}
	}

	@Test
	public void testAutodetectLazyMBeans() throws Exception {
		ConfigurableApplicationContext ctx = load("autodetectLazyMBeans.xml");
		try {
			ctx.getBean("exporter");
			MBeanServer server = ctx.getBean("server", MBeanServer.class);

			ObjectName oname = ObjectNameManager.getInstance("spring:mbean=true");
			assertThat(server.getObjectInstance(oname)).isNotNull();
			String name = (String) server.getAttribute(oname, "Name");
			assertThat(name).as("Invalid name returned").isEqualTo("Rob Harrop");

			oname = ObjectNameManager.getInstance("spring:mbean=another");
			assertThat(server.getObjectInstance(oname)).isNotNull();
			name = (String) server.getAttribute(oname, "Name");
			assertThat(name).as("Invalid name returned").isEqualTo("Juergen Hoeller");
		}
		finally {
			ctx.close();
		}
	}

	@Test
	public void testAutodetectNoMBeans() throws Exception {
		ConfigurableApplicationContext ctx = load("autodetectNoMBeans.xml");
		try {
			ctx.getBean("exporter");
		}
		finally {
			ctx.close();
		}
	}

	@Test
	public void testWithMBeanExporterListeners() throws Exception {
		MockMBeanExporterListener listener1 = new MockMBeanExporterListener();
		MockMBeanExporterListener listener2 = new MockMBeanExporterListener();

		MBeanExporter exporter = new MBeanExporter();
		exporter.setBeans(getBeanMap());
		exporter.setServer(server);
		exporter.setListeners(listener1, listener2);
		start(exporter);
		exporter.destroy();

		assertListener(listener1);
		assertListener(listener2);
	}

	@Test
	public void testExportJdkProxy() throws Exception {
		JmxTestBean bean = new JmxTestBean();
		bean.setName("Rob Harrop");

		ProxyFactory factory = new ProxyFactory();
		factory.setTarget(bean);
		factory.addAdvice(new NopInterceptor());
		factory.setInterfaces(IJmxTestBean.class);

		IJmxTestBean proxy = (IJmxTestBean) factory.getProxy();
		String name = "bean:mmm=whatever";

		Map<String, Object> beans = new HashMap<>();
		beans.put(name, proxy);

		MBeanExporter exporter = new MBeanExporter();
		exporter.setServer(server);
		exporter.setBeans(beans);
		exporter.registerBeans();

		ObjectName oname = ObjectName.getInstance(name);
		Object nameValue = server.getAttribute(oname, "Name");
		assertThat(nameValue).isEqualTo("Rob Harrop");
	}

	@Test
	public void testSelfNaming() throws Exception {
		ObjectName objectName = ObjectNameManager.getInstance(OBJECT_NAME);
		SelfNamingTestBean testBean = new SelfNamingTestBean();
		testBean.setObjectName(objectName);

		Map<String, Object> beans = new HashMap<>();
		beans.put("foo", testBean);

		MBeanExporter exporter = new MBeanExporter();
		exporter.setServer(server);
		exporter.setBeans(beans);

		start(exporter);

		ObjectInstance instance = server.getObjectInstance(objectName);
		assertThat(instance).isNotNull();
	}

	@Test
	public void testRegisterIgnoreExisting() throws Exception {
		ObjectName objectName = ObjectNameManager.getInstance(OBJECT_NAME);

		Person preRegistered = new Person();
		preRegistered.setName("Rob Harrop");

		server.registerMBean(preRegistered, objectName);

		Person springRegistered = new Person();
		springRegistered.setName("Sally Greenwood");

		String objectName2 = "spring:test=equalBean";

		Map<String, Object> beans = new HashMap<>();
		beans.put(objectName.toString(), springRegistered);
		beans.put(objectName2, springRegistered);

		MBeanExporter exporter = new MBeanExporter();
		exporter.setServer(server);
		exporter.setBeans(beans);
		exporter.setRegistrationPolicy(RegistrationPolicy.IGNORE_EXISTING);

		start(exporter);

		ObjectInstance instance = server.getObjectInstance(objectName);
		assertThat(instance).isNotNull();
		ObjectInstance instance2 = server.getObjectInstance(new ObjectName(objectName2));
		assertThat(instance2).isNotNull();

		// should still be the first bean with name Rob Harrop
		assertThat(server.getAttribute(objectName, "Name")).isEqualTo("Rob Harrop");
	}

	@Test
	public void testRegisterReplaceExisting() throws Exception {
		ObjectName objectName = ObjectNameManager.getInstance(OBJECT_NAME);

		Person preRegistered = new Person();
		preRegistered.setName("Rob Harrop");

		server.registerMBean(preRegistered, objectName);

		Person springRegistered = new Person();
		springRegistered.setName("Sally Greenwood");

		Map<String, Object> beans = new HashMap<>();
		beans.put(objectName.toString(), springRegistered);

		MBeanExporter exporter = new MBeanExporter();
		exporter.setServer(server);
		exporter.setBeans(beans);
		exporter.setRegistrationPolicy(RegistrationPolicy.REPLACE_EXISTING);

		start(exporter);

		ObjectInstance instance = server.getObjectInstance(objectName);
		assertThat(instance).isNotNull();

		// should still be the new bean with name Sally Greenwood
		assertThat(server.getAttribute(objectName, "Name")).isEqualTo("Sally Greenwood");
	}

	@Test
	public void testWithExposeClassLoader() throws Exception {
		String name = "Rob Harrop";
		String otherName = "Juergen Hoeller";

		JmxTestBean bean = new JmxTestBean();
		bean.setName(name);
		ObjectName objectName = ObjectNameManager.getInstance("spring:type=Test");

		Map<String, Object> beans = new HashMap<>();
		beans.put(objectName.toString(), bean);

		MBeanExporter exporter = new MBeanExporter();
		exporter.setServer(getServer());
		exporter.setBeans(beans);
		exporter.setExposeManagedResourceClassLoader(true);
		start(exporter);

		assertIsRegistered("Bean instance not registered", objectName);

		Object result = server.invoke(objectName, "add", new Object[] {new Integer(2), new Integer(3)}, new String[] {
				int.class.getName(), int.class.getName()});

		assertThat(new Integer(5)).as("Incorrect result return from add").isEqualTo(result);
		assertThat(server.getAttribute(objectName, "Name")).as("Incorrect attribute value").isEqualTo(name);

		server.setAttribute(objectName, new Attribute("Name", otherName));
		assertThat(bean.getName()).as("Incorrect updated name.").isEqualTo(otherName);
	}

	@Test
	public void testBonaFideMBeanIsNotExportedWhenAutodetectIsTotallyTurnedOff() throws Exception {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(Person.class);
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		factory.registerBeanDefinition("^&_invalidObjectName_(*", builder.getBeanDefinition());
		String exportedBeanName = "export.me.please";
		factory.registerSingleton(exportedBeanName, new TestBean());

		MBeanExporter exporter = new MBeanExporter();
		Map<String, Object> beansToExport = new HashMap<>();
		beansToExport.put(OBJECT_NAME, exportedBeanName);
		exporter.setBeans(beansToExport);
		exporter.setServer(getServer());
		exporter.setBeanFactory(factory);
		exporter.setAutodetectMode(MBeanExporter.AUTODETECT_NONE);
		// MBean has a bad ObjectName, so if said MBean is autodetected, an exception will be thrown...
		start(exporter);

	}

	@Test
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
		start(exporter);

		assertIsRegistered("Bona fide MBean not autodetected in AUTODETECT_MBEAN mode",
				ObjectNameManager.getInstance(OBJECT_NAME));
		assertIsNotRegistered("Bean autodetected and (only) AUTODETECT_MBEAN mode is on",
				ObjectNameManager.getInstance(exportedBeanName));
	}

	@Test
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
		start(exporter);
		assertIsRegistered("Bona fide MBean not autodetected in (AUTODETECT_ALL) mode",
				ObjectNameManager.getInstance(OBJECT_NAME));
		assertIsRegistered("Bean not autodetected in (AUTODETECT_ALL) mode",
				ObjectNameManager.getInstance(exportedBeanName));
		assertIsNotRegistered("Bean autodetected and did not satisfy the autodetect info assembler",
				ObjectNameManager.getInstance(notToBeExportedBeanName));
	}

	@Test
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
		start(exporter);
		assertIsNotRegistered("Bona fide MBean was autodetected in AUTODETECT_ASSEMBLER mode - must not have been",
				ObjectNameManager.getInstance(OBJECT_NAME));
		assertIsRegistered("Bean not autodetected in AUTODETECT_ASSEMBLER mode",
				ObjectNameManager.getInstance(exportedBeanName));
	}

	/**
	 * Want to ensure that said MBean is not exported twice.
	 */
	@Test
	public void testBonaFideMBeanExplicitlyExportedAndAutodetectionIsOn() throws Exception {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(Person.class);
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		factory.registerBeanDefinition(OBJECT_NAME, builder.getBeanDefinition());

		MBeanExporter exporter = new MBeanExporter();
		exporter.setServer(getServer());
		Map<String, Object> beansToExport = new HashMap<>();
		beansToExport.put(OBJECT_NAME, OBJECT_NAME);
		exporter.setBeans(beansToExport);
		exporter.setAssembler(new NamedBeanAutodetectCapableMBeanInfoAssemblerStub(OBJECT_NAME));
		exporter.setBeanFactory(factory);
		exporter.setAutodetectMode(MBeanExporter.AUTODETECT_ASSEMBLER);
		start(exporter);
		assertIsRegistered("Explicitly exported bona fide MBean obviously not exported.",
				ObjectNameManager.getInstance(OBJECT_NAME));
	}

	@Test
	public void testSetAutodetectModeToOutOfRangeNegativeValue() {
		MBeanExporter exporter = new MBeanExporter();
		assertThatIllegalArgumentException().isThrownBy(() ->
				exporter.setAutodetectMode(-1));
	}

	@Test
	public void testSetAutodetectModeToOutOfRangePositiveValue() {
		MBeanExporter exporter = new MBeanExporter();
		assertThatIllegalArgumentException().isThrownBy(() ->
				exporter.setAutodetectMode(5));
	}

	@Test
	public void testSetAutodetectModeNameToAnEmptyString() {
		MBeanExporter exporter = new MBeanExporter();
		assertThatIllegalArgumentException().isThrownBy(() ->
				exporter.setAutodetectModeName(""));
	}

	@Test
	public void testSetAutodetectModeNameToAWhitespacedString() {
		MBeanExporter exporter = new MBeanExporter();
		assertThatIllegalArgumentException().isThrownBy(() ->
				exporter.setAutodetectModeName("  \t"));
	}

	@Test
	public void testSetAutodetectModeNameToARubbishValue() {
		MBeanExporter exporter = new MBeanExporter();
		assertThatIllegalArgumentException().isThrownBy(() ->
				exporter.setAutodetectModeName("That Hansel is... *sssooo* hot right now!"));
	}

	@Test
	public void testNotRunningInBeanFactoryAndPassedBeanNameToExport() throws Exception {
		MBeanExporter exporter = new MBeanExporter();
		Map<String, Object> beans = new HashMap<>();
		beans.put(OBJECT_NAME, "beanName");
		exporter.setBeans(beans);
		assertThatExceptionOfType(MBeanExportException.class).isThrownBy(() ->
				start(exporter));
	}

	@Test
	public void testNotRunningInBeanFactoryAndAutodetectionIsOn() throws Exception {
		MBeanExporter exporter = new MBeanExporter();
		exporter.setAutodetectMode(MBeanExporter.AUTODETECT_ALL);
		assertThatExceptionOfType(MBeanExportException.class).isThrownBy(() ->
				start(exporter));
	}

	@Test  // SPR-2158
	public void testMBeanIsNotUnregisteredSpuriouslyIfSomeExternalProcessHasUnregisteredMBean() throws Exception {
		MBeanExporter exporter = new MBeanExporter();
		exporter.setBeans(getBeanMap());
		exporter.setServer(this.server);
		MockMBeanExporterListener listener = new MockMBeanExporterListener();
		exporter.setListeners(listener);
		start(exporter);
		assertIsRegistered("The bean was not registered with the MBeanServer",
				ObjectNameManager.getInstance(OBJECT_NAME));

		this.server.unregisterMBean(new ObjectName(OBJECT_NAME));
		exporter.destroy();
		assertThat(listener.getUnregistered().size()).as("Listener should not have been invoked (MBean previously unregistered by external agent)").isEqualTo(0);
	}

	@Test  // SPR-3302
	public void testBeanNameCanBeUsedInNotificationListenersMap() throws Exception {
		String beanName = "charlesDexterWard";
		BeanDefinitionBuilder testBean = BeanDefinitionBuilder.rootBeanDefinition(JmxTestBean.class);

		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		factory.registerBeanDefinition(beanName, testBean.getBeanDefinition());
		factory.preInstantiateSingletons();
		Object testBeanInstance = factory.getBean(beanName);

		MBeanExporter exporter = new MBeanExporter();
		exporter.setServer(getServer());
		Map<String, Object> beansToExport = new HashMap<>();
		beansToExport.put("test:what=ever", testBeanInstance);
		exporter.setBeans(beansToExport);
		exporter.setBeanFactory(factory);
		StubNotificationListener listener = new StubNotificationListener();
		exporter.setNotificationListenerMappings(Collections.singletonMap(beanName, listener));

		start(exporter);
	}

	@Test
	public void testWildcardCanBeUsedInNotificationListenersMap() throws Exception {
		String beanName = "charlesDexterWard";
		BeanDefinitionBuilder testBean = BeanDefinitionBuilder.rootBeanDefinition(JmxTestBean.class);

		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		factory.registerBeanDefinition(beanName, testBean.getBeanDefinition());
		factory.preInstantiateSingletons();
		Object testBeanInstance = factory.getBean(beanName);

		MBeanExporter exporter = new MBeanExporter();
		exporter.setServer(getServer());
		Map<String, Object> beansToExport = new HashMap<>();
		beansToExport.put("test:what=ever", testBeanInstance);
		exporter.setBeans(beansToExport);
		exporter.setBeanFactory(factory);
		StubNotificationListener listener = new StubNotificationListener();
		exporter.setNotificationListenerMappings(Collections.singletonMap("*", listener));

		start(exporter);
	}

	@Test  // SPR-3625
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
		Map<String, Object> beansToExport = new HashMap<>();
		beansToExport.put(objectName1, objectName1);
		beansToExport.put(objectName2, objectName2);
		exporter.setBeans(beansToExport);
		exporter.setBeanFactory(factory);

		assertThatExceptionOfType(RuntimeException.class).as("failed during creation of RuntimeExceptionThrowingConstructorBean").isThrownBy(() ->
				start(exporter));

		assertIsNotRegistered("Must have unregistered all previously registered MBeans due to RuntimeException",
				ObjectNameManager.getInstance(objectName1));
		assertIsNotRegistered("Must have never registered this MBean due to RuntimeException",
				ObjectNameManager.getInstance(objectName2));
	}

	@Test
	public void testIgnoreBeanName() throws MalformedObjectNameException {
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		String firstBeanName = "spring:type=TestBean";
		factory.registerSingleton(firstBeanName, new TestBean("test"));
		String secondBeanName = "spring:type=TestBean2";
		factory.registerSingleton(secondBeanName, new TestBean("test2"));

		MBeanExporter exporter = new MBeanExporter();
		exporter.setServer(getServer());
		exporter.setAssembler(new NamedBeanAutodetectCapableMBeanInfoAssemblerStub(firstBeanName, secondBeanName));
		exporter.setBeanFactory(factory);
		exporter.setAutodetectMode(MBeanExporter.AUTODETECT_ALL);
		exporter.addExcludedBean(secondBeanName);

		start(exporter);
		assertIsRegistered("Bean not autodetected in (AUTODETECT_ALL) mode",
				ObjectNameManager.getInstance(firstBeanName));
		assertIsNotRegistered("Bean should have been excluded",
				ObjectNameManager.getInstance(secondBeanName));
	}

	@Test
	public void testRegisterFactoryBean() throws MalformedObjectNameException {
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		factory.registerBeanDefinition("spring:type=FactoryBean", new RootBeanDefinition(ProperSomethingFactoryBean.class));

		MBeanExporter exporter = new MBeanExporter();
		exporter.setServer(getServer());
		exporter.setBeanFactory(factory);
		exporter.setAutodetectMode(MBeanExporter.AUTODETECT_ALL);

		start(exporter);
		assertIsRegistered("Non-null FactoryBean object registered",
				ObjectNameManager.getInstance("spring:type=FactoryBean"));
	}

	@Test
	public void testIgnoreNullObjectFromFactoryBean() throws MalformedObjectNameException {
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		factory.registerBeanDefinition("spring:type=FactoryBean", new RootBeanDefinition(NullSomethingFactoryBean.class));

		MBeanExporter exporter = new MBeanExporter();
		exporter.setServer(getServer());
		exporter.setBeanFactory(factory);
		exporter.setAutodetectMode(MBeanExporter.AUTODETECT_ALL);

		start(exporter);
		assertIsNotRegistered("Null FactoryBean object not registered",
				ObjectNameManager.getInstance("spring:type=FactoryBean"));
	}


	private ConfigurableApplicationContext load(String context) {
		return new ClassPathXmlApplicationContext(context, getClass());
	}

	private Map<String, Object> getBeanMap() {
		Map<String, Object> map = new HashMap<>();
		map.put(OBJECT_NAME, new JmxTestBean());
		return map;
	}

	private void assertListener(MockMBeanExporterListener listener) throws MalformedObjectNameException {
		ObjectName desired = ObjectNameManager.getInstance(OBJECT_NAME);
		assertThat(listener.getRegistered().size()).as("Incorrect number of registrations").isEqualTo(1);
		assertThat(listener.getUnregistered().size()).as("Incorrect number of unregistrations").isEqualTo(1);
		assertThat(listener.getRegistered().get(0)).as("Incorrect ObjectName in register").isEqualTo(desired);
		assertThat(listener.getUnregistered().get(0)).as("Incorrect ObjectName in unregister").isEqualTo(desired);
	}


	private static class InvokeDetectAssembler implements MBeanInfoAssembler {

		private boolean invoked = false;

		@Override
		public ModelMBeanInfo getMBeanInfo(Object managedResource, String beanKey) throws JMException {
			invoked = true;
			return null;
		}
	}


	private static class MockMBeanExporterListener implements MBeanExporterListener {

		private List<ObjectName> registered = new ArrayList<>();

		private List<ObjectName> unregistered = new ArrayList<>();

		@Override
		public void mbeanRegistered(ObjectName objectName) {
			registered.add(objectName);
		}

		@Override
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

		@Override
		public ObjectName getObjectName() throws MalformedObjectNameException {
			return this.objectName;
		}
	}


	public static interface PersonMBean {

		String getName();
	}


	public static class Person implements PersonMBean {

		private String name;

		@Override
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}


	public static final class StubNotificationListener implements NotificationListener {

		private List<Notification> notifications = new ArrayList<>();

		@Override
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

		private Collection<String> namedBeans;

		public NamedBeanAutodetectCapableMBeanInfoAssemblerStub(String... namedBeans) {
			this.namedBeans = Arrays.asList(namedBeans);
		}

		@Override
		public boolean includeBean(Class<?> beanClass, String beanName) {
			return this.namedBeans.contains(beanName);
		}
	}


	public interface SomethingMBean {}

	public static class Something implements SomethingMBean {}


	public static class ProperSomethingFactoryBean implements FactoryBean<Something> {

		@Override public Something getObject() {
			return new Something();
		}

		@Override public Class<?> getObjectType() {
			return Something.class;
		}

		@Override public boolean isSingleton() {
			return true;
		}
	}


	public static class NullSomethingFactoryBean implements FactoryBean<Something> {

		@Override public Something getObject() {
			return null;
		}

		@Override public Class<?> getObjectType() {
			return Something.class;
		}

		@Override public boolean isSingleton() {
			return true;
		}
	}

}
