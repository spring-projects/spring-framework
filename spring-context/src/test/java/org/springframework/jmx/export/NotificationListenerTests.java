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

package org.springframework.jmx.export;

import java.util.HashMap;
import java.util.Map;

import javax.management.Attribute;
import javax.management.AttributeChangeNotification;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.jmx.AbstractMBeanServerTests;
import org.springframework.jmx.JmxTestBean;
import org.springframework.jmx.access.NotificationListenerRegistrar;
import org.springframework.jmx.export.naming.SelfNaming;
import org.springframework.jmx.support.ObjectNameManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Rob Harrop
 * @author Mark Fisher
 * @author Sam Brannen
 */
class NotificationListenerTests extends AbstractMBeanServerTests {

	@SuppressWarnings({"rawtypes", "unchecked"})
	@Test
	void testRegisterNotificationListenerForMBean() throws Exception {
		ObjectName objectName = ObjectName.getInstance("spring:name=Test");
		JmxTestBean bean = new JmxTestBean();

		Map<String, Object> beans = new HashMap<>();
		beans.put(objectName.getCanonicalName(), bean);

		CountingAttributeChangeNotificationListener listener = new CountingAttributeChangeNotificationListener();

		Map notificationListeners = new HashMap();
		notificationListeners.put(objectName, listener);

		MBeanExporter exporter = new MBeanExporter();
		exporter.setServer(server);
		exporter.setBeans(beans);
		exporter.setNotificationListenerMappings(notificationListeners);
		start(exporter);

		// update the attribute
		String attributeName = "Name";
		server.setAttribute(objectName, new Attribute(attributeName, "Rob Harrop"));
		assertThat(listener.getCount(attributeName)).as("Listener not notified").isEqualTo(1);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	void testRegisterNotificationListenerWithWildcard() throws Exception {
		ObjectName objectName = ObjectName.getInstance("spring:name=Test");
		JmxTestBean bean = new JmxTestBean();

		Map<String, Object> beans = new HashMap<>();
		beans.put(objectName.getCanonicalName(), bean);

		CountingAttributeChangeNotificationListener listener = new CountingAttributeChangeNotificationListener();

		Map notificationListeners = new HashMap();
		notificationListeners.put("*", listener);

		MBeanExporter exporter = new MBeanExporter();
		exporter.setServer(server);
		exporter.setBeans(beans);
		exporter.setNotificationListenerMappings(notificationListeners);
		start(exporter);

		// update the attribute
		String attributeName = "Name";
		server.setAttribute(objectName, new Attribute(attributeName, "Rob Harrop"));
		assertThat(listener.getCount(attributeName)).as("Listener not notified").isEqualTo(1);
	}

	@Test
	void testRegisterNotificationListenerWithHandback() throws Exception {
		String objectName = "spring:name=Test";
		JmxTestBean bean = new JmxTestBean();

		Map<String, Object> beans = new HashMap<>();
		beans.put(objectName, bean);

		CountingAttributeChangeNotificationListener listener = new CountingAttributeChangeNotificationListener();
		Object handback = new Object();

		NotificationListenerBean listenerBean = new NotificationListenerBean();
		listenerBean.setNotificationListener(listener);
		listenerBean.setMappedObjectName("spring:name=Test");
		listenerBean.setHandback(handback);

		MBeanExporter exporter = new MBeanExporter();
		exporter.setServer(server);
		exporter.setBeans(beans);
		exporter.setNotificationListeners(listenerBean);
		start(exporter);

		// update the attribute
		String attributeName = "Name";
		server.setAttribute(ObjectNameManager.getInstance("spring:name=Test"), new Attribute(attributeName,
				"Rob Harrop"));

		assertThat(listener.getCount(attributeName)).as("Listener not notified").isEqualTo(1);
		assertThat(listener.getLastHandback(attributeName)).as("Handback object not transmitted correctly").isEqualTo(handback);
	}

	@Test
	void testRegisterNotificationListenerForAllMBeans() throws Exception {
		ObjectName objectName = ObjectName.getInstance("spring:name=Test");
		JmxTestBean bean = new JmxTestBean();

		Map<String, Object> beans = new HashMap<>();
		beans.put(objectName.getCanonicalName(), bean);

		CountingAttributeChangeNotificationListener listener = new CountingAttributeChangeNotificationListener();

		NotificationListenerBean listenerBean = new NotificationListenerBean();
		listenerBean.setNotificationListener(listener);

		MBeanExporter exporter = new MBeanExporter();
		exporter.setServer(server);
		exporter.setBeans(beans);
		exporter.setNotificationListeners(listenerBean);
		start(exporter);

		// update the attribute
		String attributeName = "Name";
		server.setAttribute(objectName, new Attribute(attributeName, "Rob Harrop"));

		assertThat(listener.getCount(attributeName)).as("Listener not notified").isEqualTo(1);
	}

	@SuppressWarnings("serial")
	@Test
	void testRegisterNotificationListenerWithFilter() throws Exception {
		ObjectName objectName = ObjectName.getInstance("spring:name=Test");
		JmxTestBean bean = new JmxTestBean();

		Map<String, Object> beans = new HashMap<>();
		beans.put(objectName.getCanonicalName(), bean);

		CountingAttributeChangeNotificationListener listener = new CountingAttributeChangeNotificationListener();

		NotificationListenerBean listenerBean = new NotificationListenerBean();
		listenerBean.setNotificationListener(listener);
		listenerBean.setNotificationFilter(notification -> {
			if (notification instanceof AttributeChangeNotification changeNotification) {
				return "Name".equals(changeNotification.getAttributeName());
			}
			else {
				return false;
			}
		});

		MBeanExporter exporter = new MBeanExporter();
		exporter.setServer(server);
		exporter.setBeans(beans);
		exporter.setNotificationListeners(listenerBean);
		start(exporter);

		// update the attributes
		String nameAttribute = "Name";
		String ageAttribute = "Age";

		server.setAttribute(objectName, new Attribute(nameAttribute, "Rob Harrop"));
		server.setAttribute(objectName, new Attribute(ageAttribute, 90));

		assertThat(listener.getCount(nameAttribute)).as("Listener not notified for Name").isEqualTo(1);
		assertThat(listener.getCount(ageAttribute)).as("Listener incorrectly notified for Age").isEqualTo(0);
	}

	@Test
	void testCreationWithNoNotificationListenerSet() {
		assertThatIllegalArgumentException().as("no NotificationListener supplied").isThrownBy(
				new NotificationListenerBean()::afterPropertiesSet);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	void testRegisterNotificationListenerWithBeanNameAndBeanNameInBeansMap() throws Exception {
		String beanName = "testBean";
		ObjectName objectName = ObjectName.getInstance("spring:name=Test");

		SelfNamingTestBean testBean = new SelfNamingTestBean();
		testBean.setObjectName(objectName);

		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		factory.registerSingleton(beanName, testBean);

		Map<String, Object> beans = new HashMap<>();
		beans.put(beanName, beanName);

		Map listenerMappings = new HashMap();
		CountingAttributeChangeNotificationListener listener = new CountingAttributeChangeNotificationListener();
		listenerMappings.put(beanName, listener);

		MBeanExporter exporter = new MBeanExporter();
		exporter.setServer(server);
		exporter.setBeans(beans);
		exporter.setNotificationListenerMappings(listenerMappings);
		exporter.setBeanFactory(factory);
		start(exporter);
		assertIsRegistered("Should have registered MBean", objectName);

		server.setAttribute(objectName, new Attribute("Age", 77));
		assertThat(listener.getCount("Age")).as("Listener not notified").isEqualTo(1);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	void testRegisterNotificationListenerWithBeanNameAndBeanInstanceInBeansMap() throws Exception {
		String beanName = "testBean";
		ObjectName objectName = ObjectName.getInstance("spring:name=Test");

		SelfNamingTestBean testBean = new SelfNamingTestBean();
		testBean.setObjectName(objectName);

		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		factory.registerSingleton(beanName, testBean);

		Map<String, Object> beans = new HashMap<>();
		beans.put(beanName, testBean);

		Map listenerMappings = new HashMap();
		CountingAttributeChangeNotificationListener listener = new CountingAttributeChangeNotificationListener();
		listenerMappings.put(beanName, listener);

		MBeanExporter exporter = new MBeanExporter();
		exporter.setServer(server);
		exporter.setBeans(beans);
		exporter.setNotificationListenerMappings(listenerMappings);
		exporter.setBeanFactory(factory);
		start(exporter);
		assertIsRegistered("Should have registered MBean", objectName);

		server.setAttribute(objectName, new Attribute("Age", 77));
		assertThat(listener.getCount("Age")).as("Listener not notified").isEqualTo(1);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	void testRegisterNotificationListenerWithBeanNameBeforeObjectNameMappedToSameBeanInstance() throws Exception {
		String beanName = "testBean";
		ObjectName objectName = ObjectName.getInstance("spring:name=Test");

		SelfNamingTestBean testBean = new SelfNamingTestBean();
		testBean.setObjectName(objectName);

		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		factory.registerSingleton(beanName, testBean);

		Map<String, Object> beans = new HashMap<>();
		beans.put(beanName, testBean);

		Map listenerMappings = new HashMap();
		CountingAttributeChangeNotificationListener listener = new CountingAttributeChangeNotificationListener();
		listenerMappings.put(beanName, listener);
		listenerMappings.put(objectName, listener);

		MBeanExporter exporter = new MBeanExporter();
		exporter.setServer(server);
		exporter.setBeans(beans);
		exporter.setNotificationListenerMappings(listenerMappings);
		exporter.setBeanFactory(factory);
		start(exporter);
		assertIsRegistered("Should have registered MBean", objectName);

		server.setAttribute(objectName, new Attribute("Age", 77));
		assertThat(listener.getCount("Age")).as("Listener should have been notified exactly once").isEqualTo(1);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	void testRegisterNotificationListenerWithObjectNameBeforeBeanNameMappedToSameBeanInstance() throws Exception {
		String beanName = "testBean";
		ObjectName objectName = ObjectName.getInstance("spring:name=Test");

		SelfNamingTestBean testBean = new SelfNamingTestBean();
		testBean.setObjectName(objectName);

		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		factory.registerSingleton(beanName, testBean);

		Map<String, Object> beans = new HashMap<>();
		beans.put(beanName, testBean);

		Map listenerMappings = new HashMap();
		CountingAttributeChangeNotificationListener listener = new CountingAttributeChangeNotificationListener();
		listenerMappings.put(objectName, listener);
		listenerMappings.put(beanName, listener);

		MBeanExporter exporter = new MBeanExporter();
		exporter.setServer(server);
		exporter.setBeans(beans);
		exporter.setNotificationListenerMappings(listenerMappings);
		exporter.setBeanFactory(factory);
		start(exporter);
		assertIsRegistered("Should have registered MBean", objectName);

		server.setAttribute(objectName, new Attribute("Age", 77));
		assertThat(listener.getCount("Age")).as("Listener should have been notified exactly once").isEqualTo(1);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	void testRegisterNotificationListenerWithTwoBeanNamesMappedToDifferentBeanInstances() throws Exception {
		String beanName1 = "testBean1";
		String beanName2 = "testBean2";

		ObjectName objectName1 = ObjectName.getInstance("spring:name=Test1");
		ObjectName objectName2 = ObjectName.getInstance("spring:name=Test2");

		SelfNamingTestBean testBean1 = new SelfNamingTestBean();
		testBean1.setObjectName(objectName1);

		SelfNamingTestBean testBean2 = new SelfNamingTestBean();
		testBean2.setObjectName(objectName2);

		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		factory.registerSingleton(beanName1, testBean1);
		factory.registerSingleton(beanName2, testBean2);

		Map<String, Object> beans = new HashMap<>();
		beans.put(beanName1, testBean1);
		beans.put(beanName2, testBean2);

		Map listenerMappings = new HashMap();
		CountingAttributeChangeNotificationListener listener = new CountingAttributeChangeNotificationListener();
		listenerMappings.put(beanName1, listener);
		listenerMappings.put(beanName2, listener);

		MBeanExporter exporter = new MBeanExporter();
		exporter.setServer(server);
		exporter.setBeans(beans);
		exporter.setNotificationListenerMappings(listenerMappings);
		exporter.setBeanFactory(factory);
		start(exporter);
		assertIsRegistered("Should have registered MBean", objectName1);
		assertIsRegistered("Should have registered MBean", objectName2);

		server.setAttribute(ObjectNameManager.getInstance(objectName1), new Attribute("Age", 77));
		assertThat(listener.getCount("Age")).as("Listener not notified for testBean1").isEqualTo(1);

		server.setAttribute(ObjectNameManager.getInstance(objectName2), new Attribute("Age", 33));
		assertThat(listener.getCount("Age")).as("Listener not notified for testBean2").isEqualTo(2);
	}

	@Test
	void testNotificationListenerRegistrar() throws Exception {
		ObjectName objectName = ObjectName.getInstance("spring:name=Test");
		JmxTestBean bean = new JmxTestBean();

		Map<String, Object> beans = new HashMap<>();
		beans.put(objectName.getCanonicalName(), bean);

		MBeanExporter exporter = new MBeanExporter();
		exporter.setServer(server);
		exporter.setBeans(beans);
		start(exporter);

		CountingAttributeChangeNotificationListener listener = new CountingAttributeChangeNotificationListener();

		NotificationListenerRegistrar registrar = new NotificationListenerRegistrar();
		registrar.setServer(server);
		registrar.setNotificationListener(listener);
		registrar.setMappedObjectName(objectName);
		registrar.afterPropertiesSet();

		// update the attribute
		String attributeName = "Name";
		server.setAttribute(objectName, new Attribute(attributeName, "Rob Harrop"));
		assertThat(listener.getCount(attributeName)).as("Listener not notified").isEqualTo(1);

		registrar.destroy();

		// try to update the attribute again
		server.setAttribute(objectName, new Attribute(attributeName, "Rob Harrop"));
		assertThat(listener.getCount(attributeName)).as("Listener notified after destruction").isEqualTo(1);
	}

	@Test
	void testNotificationListenerRegistrarWithMultipleNames() throws Exception {
		ObjectName objectName = ObjectName.getInstance("spring:name=Test");
		ObjectName objectName2 = ObjectName.getInstance("spring:name=Test2");
		JmxTestBean bean = new JmxTestBean();
		JmxTestBean bean2 = new JmxTestBean();

		Map<String, Object> beans = new HashMap<>();
		beans.put(objectName.getCanonicalName(), bean);
		beans.put(objectName2.getCanonicalName(), bean2);

		MBeanExporter exporter = new MBeanExporter();
		exporter.setServer(server);
		exporter.setBeans(beans);
		start(exporter);

		CountingAttributeChangeNotificationListener listener = new CountingAttributeChangeNotificationListener();

		NotificationListenerRegistrar registrar = new NotificationListenerRegistrar();
		registrar.setServer(server);
		registrar.setNotificationListener(listener);
		//registrar.setMappedObjectNames(new Object[] {objectName, objectName2});
		registrar.setMappedObjectNames("spring:name=Test", "spring:name=Test2");
		registrar.afterPropertiesSet();

		// update the attribute
		String attributeName = "Name";
		server.setAttribute(objectName, new Attribute(attributeName, "Rob Harrop"));
		assertThat(listener.getCount(attributeName)).as("Listener not notified").isEqualTo(1);

		registrar.destroy();

		// try to update the attribute again
		server.setAttribute(objectName, new Attribute(attributeName, "Rob Harrop"));
		assertThat(listener.getCount(attributeName)).as("Listener notified after destruction").isEqualTo(1);
	}


	@SuppressWarnings({"rawtypes", "unchecked"})
	private static class CountingAttributeChangeNotificationListener implements NotificationListener {

		private Map attributeCounts = new HashMap();

		private Map attributeHandbacks = new HashMap();

		@Override
		public void handleNotification(Notification notification, Object handback) {
			if (notification instanceof AttributeChangeNotification attNotification) {
				String attributeName = attNotification.getAttributeName();

				Integer currentCount = (Integer) this.attributeCounts.get(attributeName);

				if (currentCount != null) {
					int count = currentCount + 1;
					this.attributeCounts.put(attributeName, count);
				}
				else {
					this.attributeCounts.put(attributeName, 1);
				}

				this.attributeHandbacks.put(attributeName, handback);
			}
		}

		public int getCount(String attribute) {
			Integer count = (Integer) this.attributeCounts.get(attribute);
			return (count == null ? 0 : count);
		}

		public Object getLastHandback(String attributeName) {
			return this.attributeHandbacks.get(attributeName);
		}
	}


	public static class SelfNamingTestBean implements SelfNaming {

		private ObjectName objectName;

		private int age;

		public void setObjectName(ObjectName objectName) {
			this.objectName = objectName;
		}

		@Override
		public ObjectName getObjectName() {
			return this.objectName;
		}

		public void setAge(int age) {
			this.age = age;
		}

		public int getAge() {
			return this.age;
		}
	}

}
