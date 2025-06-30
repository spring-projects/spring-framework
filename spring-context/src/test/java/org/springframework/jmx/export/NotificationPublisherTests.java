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

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.DynamicMBean;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationListener;

import org.junit.jupiter.api.Test;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jmx.AbstractMBeanServerTests;
import org.springframework.jmx.export.notification.NotificationPublisher;
import org.springframework.jmx.export.notification.NotificationPublisherAware;
import org.springframework.jmx.support.ObjectNameManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Spring JMX {@link NotificationPublisher} functionality.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
class NotificationPublisherTests extends AbstractMBeanServerTests {

	private CountingNotificationListener listener = new CountingNotificationListener();

	@Test
	void testSimpleBean() throws Exception {
		// start the MBeanExporter
		ConfigurableApplicationContext ctx = loadContext("org/springframework/jmx/export/notificationPublisherTests.xml");
		this.server.addNotificationListener(ObjectNameManager.getInstance("spring:type=Publisher"), listener, null,
				null);

		MyNotificationPublisher publisher = (MyNotificationPublisher) ctx.getBean("publisher");
		assertThat(publisher.getNotificationPublisher()).as("NotificationPublisher should not be null").isNotNull();
		publisher.sendNotification();
		assertThat(listener.count).as("Notification not sent").isEqualTo(1);
	}

	@Test
	void testSimpleBeanRegisteredManually() throws Exception {
		// start the MBeanExporter
		ConfigurableApplicationContext ctx = loadContext("org/springframework/jmx/export/notificationPublisherTests.xml");
		MBeanExporter exporter = (MBeanExporter) ctx.getBean("exporter");
		MyNotificationPublisher publisher = new MyNotificationPublisher();
		exporter.registerManagedResource(publisher, ObjectNameManager.getInstance("spring:type=Publisher2"));
		this.server.addNotificationListener(ObjectNameManager.getInstance("spring:type=Publisher2"), listener, null,
				null);

		assertThat(publisher.getNotificationPublisher()).as("NotificationPublisher should not be null").isNotNull();
		publisher.sendNotification();
		assertThat(listener.count).as("Notification not sent").isEqualTo(1);
	}

	@Test
	void testMBean() throws Exception {
		// start the MBeanExporter
		ConfigurableApplicationContext ctx = loadContext("org/springframework/jmx/export/notificationPublisherTests.xml");
		this.server.addNotificationListener(ObjectNameManager.getInstance("spring:type=PublisherMBean"), listener,
				null, null);

		MyNotificationPublisherMBean publisher = (MyNotificationPublisherMBean) ctx.getBean("publisherMBean");
		publisher.sendNotification();
		assertThat(listener.count).as("Notification not sent").isEqualTo(1);
	}

	/*
	@Test
	void testStandardMBean() throws Exception {
		// start the MBeanExporter
		ApplicationContext ctx = new ClassPathXmlApplicationContext("org/springframework/jmx/export/notificationPublisherTests.xml");
		this.server.addNotificationListener(ObjectNameManager.getInstance("spring:type=PublisherStandardMBean"), listener, null, null);

		MyNotificationPublisherStandardMBean publisher = (MyNotificationPublisherStandardMBean) ctx.getBean("publisherStandardMBean");
		publisher.sendNotification();
		assertEquals("Notification not sent", 1, listener.count);
	}
	*/

	@Test
	void testLazyInit() throws Exception {
		// start the MBeanExporter
		ConfigurableApplicationContext ctx = loadContext("org/springframework/jmx/export/notificationPublisherLazyTests.xml");
		assertThat(ctx.getBeanFactory().containsSingleton("publisher")).as("Should not have instantiated the bean yet").isFalse();

		// need to touch the MBean proxy
		server.getAttribute(ObjectNameManager.getInstance("spring:type=Publisher"), "Name");
		this.server.addNotificationListener(ObjectNameManager.getInstance("spring:type=Publisher"), listener, null,
				null);

		MyNotificationPublisher publisher = (MyNotificationPublisher) ctx.getBean("publisher");
		assertThat(publisher.getNotificationPublisher()).as("NotificationPublisher should not be null").isNotNull();
		publisher.sendNotification();
		assertThat(listener.count).as("Notification not sent").isEqualTo(1);
	}

	private static class CountingNotificationListener implements NotificationListener {

		private int count;

		private Notification lastNotification;

		@Override
		public void handleNotification(Notification notification, Object handback) {
			this.lastNotification = notification;
			this.count++;
		}

		@SuppressWarnings("unused")
		public int getCount() {
			return count;
		}

		@SuppressWarnings("unused")
		public Notification getLastNotification() {
			return lastNotification;
		}
	}

	public static class MyNotificationPublisher implements NotificationPublisherAware {

		private NotificationPublisher notificationPublisher;

		@Override
		public void setNotificationPublisher(NotificationPublisher notificationPublisher) {
			this.notificationPublisher = notificationPublisher;
		}

		public NotificationPublisher getNotificationPublisher() {
			return notificationPublisher;
		}

		public void sendNotification() {
			this.notificationPublisher.sendNotification(new Notification("test", this, 1));
		}

		public String getName() {
			return "Rob Harrop";
		}
	}

	public static class MyNotificationPublisherMBean extends NotificationBroadcasterSupport implements DynamicMBean {

		@Override
		public Object getAttribute(String attribute) {
			return null;
		}

		@Override
		public void setAttribute(Attribute attribute) {
		}

		@Override
		public AttributeList getAttributes(String[] attributes) {
			return null;
		}

		@Override
		public AttributeList setAttributes(AttributeList attributes) {
			return null;
		}

		@Override
		public Object invoke(String actionName, Object[] params, String[] signature) {
			return null;
		}

		@Override
		public MBeanInfo getMBeanInfo() {
			return new MBeanInfo(MyNotificationPublisherMBean.class.getName(), "", new MBeanAttributeInfo[0],
					new MBeanConstructorInfo[0], new MBeanOperationInfo[0], new MBeanNotificationInfo[0]);
		}

		public void sendNotification() {
			sendNotification(new Notification("test", this, 1));
		}
	}

	public static class MyNotificationPublisherStandardMBean extends NotificationBroadcasterSupport implements MyMBean {

		@Override
		public void sendNotification() {
			sendNotification(new Notification("test", this, 1));
		}
	}

	public interface MyMBean {

		void sendNotification();
	}

}
