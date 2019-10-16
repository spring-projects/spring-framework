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

package org.springframework.jmx.export.notification;

import javax.management.AttributeChangeNotification;
import javax.management.MBeanException;
import javax.management.MalformedObjectNameException;
import javax.management.Notification;
import javax.management.ObjectName;
import javax.management.RuntimeOperationsException;

import org.junit.jupiter.api.Test;

import org.springframework.jmx.export.SpringModelMBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Rick Evans
 * @author Chris Beams
 */
public class ModelMBeanNotificationPublisherTests {

	@Test
	public void testCtorWithNullMBean() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new ModelMBeanNotificationPublisher(null, createObjectName(), this));
	}

	@Test
	public void testCtorWithNullObjectName() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new ModelMBeanNotificationPublisher(new SpringModelMBean(), null, this));
	}

	@Test
	public void testCtorWithNullManagedResource() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new ModelMBeanNotificationPublisher(new SpringModelMBean(), createObjectName(), null));
	}

	@Test
	public void testSendNullNotification() throws Exception {
		NotificationPublisher publisher
				= new ModelMBeanNotificationPublisher(new SpringModelMBean(), createObjectName(), this);
		assertThatIllegalArgumentException().isThrownBy(() ->
				publisher.sendNotification(null));
	}

	public void testSendVanillaNotification() throws Exception {
		StubSpringModelMBean mbean = new StubSpringModelMBean();
		Notification notification = new Notification("network.alarm.router", mbean, 1872);
		ObjectName objectName = createObjectName();

		NotificationPublisher publisher = new ModelMBeanNotificationPublisher(mbean, objectName, mbean);
		publisher.sendNotification(notification);

		assertThat(mbean.getActualNotification()).isNotNull();
		assertThat(mbean.getActualNotification()).as("The exact same Notification is not being passed through from the publisher to the mbean.").isSameAs(notification);
		assertThat(mbean.getActualNotification().getSource()).as("The 'source' property of the Notification is not being set to the ObjectName of the associated MBean.").isSameAs(objectName);
	}

	public void testSendAttributeChangeNotification() throws Exception {
		StubSpringModelMBean mbean = new StubSpringModelMBean();
		Notification notification = new AttributeChangeNotification(mbean, 1872, System.currentTimeMillis(), "Shall we break for some tea?", "agree", "java.lang.Boolean", Boolean.FALSE, Boolean.TRUE);
		ObjectName objectName = createObjectName();

		NotificationPublisher publisher = new ModelMBeanNotificationPublisher(mbean, objectName, mbean);
		publisher.sendNotification(notification);

		assertThat(mbean.getActualNotification()).isNotNull();
		boolean condition = mbean.getActualNotification() instanceof AttributeChangeNotification;
		assertThat(condition).isTrue();
		assertThat(mbean.getActualNotification()).as("The exact same Notification is not being passed through from the publisher to the mbean.").isSameAs(notification);
		assertThat(mbean.getActualNotification().getSource()).as("The 'source' property of the Notification is not being set to the ObjectName of the associated MBean.").isSameAs(objectName);
	}

	public void testSendAttributeChangeNotificationWhereSourceIsNotTheManagedResource() throws Exception {
		StubSpringModelMBean mbean = new StubSpringModelMBean();
		Notification notification = new AttributeChangeNotification(this, 1872, System.currentTimeMillis(), "Shall we break for some tea?", "agree", "java.lang.Boolean", Boolean.FALSE, Boolean.TRUE);
		ObjectName objectName = createObjectName();

		NotificationPublisher publisher = new ModelMBeanNotificationPublisher(mbean, objectName, mbean);
		publisher.sendNotification(notification);

		assertThat(mbean.getActualNotification()).isNotNull();
		boolean condition = mbean.getActualNotification() instanceof AttributeChangeNotification;
		assertThat(condition).isTrue();
		assertThat(mbean.getActualNotification()).as("The exact same Notification is not being passed through from the publisher to the mbean.").isSameAs(notification);
		assertThat(mbean.getActualNotification().getSource()).as("The 'source' property of the Notification is *wrongly* being set to the ObjectName of the associated MBean.").isSameAs(this);
	}

	private static ObjectName createObjectName() throws MalformedObjectNameException {
		return ObjectName.getInstance("foo:type=bar");
	}


	private static class StubSpringModelMBean extends SpringModelMBean {

		private Notification actualNotification;

		public StubSpringModelMBean() throws MBeanException, RuntimeOperationsException {
		}

		public Notification getActualNotification() {
			return this.actualNotification;
		}

		@Override
		public void sendNotification(Notification notification) throws RuntimeOperationsException {
			this.actualNotification = notification;
		}

		@Override
		public void sendAttributeChangeNotification(AttributeChangeNotification notification) throws RuntimeOperationsException {
			this.actualNotification = notification;
		}
	}

}
