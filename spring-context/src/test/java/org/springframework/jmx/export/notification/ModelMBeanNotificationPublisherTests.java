/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
class ModelMBeanNotificationPublisherTests {

	@Test
	void ctorWithNullMBean() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new ModelMBeanNotificationPublisher(null, createObjectName(), this));
	}

	@Test
	void ctorWithNullObjectName() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new ModelMBeanNotificationPublisher(new SpringModelMBean(), null, this));
	}

	@Test
	void ctorWithNullManagedResource() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new ModelMBeanNotificationPublisher(new SpringModelMBean(), createObjectName(), null));
	}

	@Test
	void sendNullNotification() throws Exception {
		NotificationPublisher publisher
				= new ModelMBeanNotificationPublisher(new SpringModelMBean(), createObjectName(), this);
		assertThatIllegalArgumentException().isThrownBy(() ->
				publisher.sendNotification(null));
	}

	@Test
	void sendVanillaNotification() throws Exception {
		StubSpringModelMBean mbean = new StubSpringModelMBean();
		Notification notification = new Notification("network.alarm.router", mbean, 1872);
		ObjectName objectName = createObjectName();

		NotificationPublisher publisher = new ModelMBeanNotificationPublisher(mbean, objectName, mbean);
		publisher.sendNotification(notification);

		assertThat(mbean.getActualNotification()).isNotNull();
		assertThat(mbean.getActualNotification()).as("The exact same Notification is not being passed through from the publisher to the mbean.").isSameAs(notification);
		assertThat(mbean.getActualNotification().getSource()).as("The 'source' property of the Notification is not being set to the ObjectName of the associated MBean.").isSameAs(objectName);
	}

	@Test
	void sendAttributeChangeNotification() throws Exception {
		StubSpringModelMBean mbean = new StubSpringModelMBean();
		Notification notification = new AttributeChangeNotification(mbean, 1872, System.currentTimeMillis(), "Shall we break for some tea?", "agree", "java.lang.Boolean", Boolean.FALSE, Boolean.TRUE);
		ObjectName objectName = createObjectName();

		NotificationPublisher publisher = new ModelMBeanNotificationPublisher(mbean, objectName, mbean);
		publisher.sendNotification(notification);

		assertThat(mbean.getActualNotification()).isNotNull();
		assertThat(mbean.getActualNotification()).isInstanceOf(AttributeChangeNotification.class);
		assertThat(mbean.getActualNotification()).as("The exact same Notification is not being passed through from the publisher to the mbean.").isSameAs(notification);
		assertThat(mbean.getActualNotification().getSource()).as("The 'source' property of the Notification is not being set to the ObjectName of the associated MBean.").isSameAs(objectName);
	}

	@Test
	void sendAttributeChangeNotificationWhereSourceIsNotTheManagedResource() throws Exception {
		StubSpringModelMBean mbean = new StubSpringModelMBean();
		Notification notification = new AttributeChangeNotification(this, 1872, System.currentTimeMillis(), "Shall we break for some tea?", "agree", "java.lang.Boolean", Boolean.FALSE, Boolean.TRUE);
		ObjectName objectName = createObjectName();

		NotificationPublisher publisher = new ModelMBeanNotificationPublisher(mbean, objectName, mbean);
		publisher.sendNotification(notification);

		assertThat(mbean.getActualNotification()).isNotNull();
		assertThat(mbean.getActualNotification()).isInstanceOf(AttributeChangeNotification.class);
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
