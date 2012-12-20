/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.jmx.export.notification;

import javax.management.AttributeChangeNotification;
import javax.management.MBeanException;
import javax.management.Notification;
import javax.management.ObjectName;
import javax.management.modelmbean.ModelMBean;
import javax.management.modelmbean.ModelMBeanNotificationBroadcaster;

import org.springframework.util.Assert;

/**
 * {@link NotificationPublisher} implementation that uses the infrastructure
 * provided by the {@link ModelMBean} interface to track
 * {@link javax.management.NotificationListener javax.management.NotificationListeners}
 * and send {@link Notification Notifications} to those listeners.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Rick Evans
 * @since 2.0
 * @see javax.management.modelmbean.ModelMBeanNotificationBroadcaster
 * @see NotificationPublisherAware
 */
public class ModelMBeanNotificationPublisher implements NotificationPublisher {

	/**
	 * The {@link ModelMBean} instance wrapping the managed resource into which this
	 * <code>NotificationPublisher</code> will be injected.
	 */
	private final ModelMBeanNotificationBroadcaster modelMBean;

	/**
	 * The {@link ObjectName} associated with the {@link ModelMBean modelMBean}.
	 */
	private final ObjectName objectName;

	/**
	 * The managed resource associated with the {@link ModelMBean modelMBean}.
	 */
	private final Object managedResource;


	/**
	 * Create a new instance of the {@link ModelMBeanNotificationPublisher} class
	 * that will publish all {@link javax.management.Notification Notifications}
	 * to the supplied {@link ModelMBean}.
	 * @param modelMBean the target {@link ModelMBean}; must not be <code>null</code>
	 * @param objectName the {@link ObjectName} of the source {@link ModelMBean}
	 * @param managedResource the managed resource exposed by the supplied {@link ModelMBean}
	 * @throws IllegalArgumentException if any of the parameters is <code>null</code>
	 */
	public ModelMBeanNotificationPublisher(
			ModelMBeanNotificationBroadcaster modelMBean, ObjectName objectName, Object managedResource) {

		Assert.notNull(modelMBean, "'modelMBean' must not be null");
		Assert.notNull(objectName, "'objectName' must not be null");
		Assert.notNull(managedResource, "'managedResource' must not be null");
		this.modelMBean = modelMBean;
		this.objectName = objectName;
		this.managedResource = managedResource;
	}


	/**
	 * Send the supplied {@link Notification} using the wrapped
	 * {@link ModelMBean} instance.
	 * @param notification the {@link Notification} to be sent
	 * @throws IllegalArgumentException if the supplied <code>notification</code> is <code>null</code>
	 * @throws UnableToSendNotificationException if the supplied <code>notification</code> could not be sent
	 */
	public void sendNotification(Notification notification) {
		Assert.notNull(notification, "Notification must not be null");
		replaceNotificationSourceIfNecessary(notification);
		try {
			if (notification instanceof AttributeChangeNotification) {
				this.modelMBean.sendAttributeChangeNotification((AttributeChangeNotification) notification);
			}
			else {
				this.modelMBean.sendNotification(notification);
			}
		}
		catch (MBeanException ex) {
			throw new UnableToSendNotificationException("Unable to send notification [" + notification + "]", ex);
		}
	}

	/**
	 * From the {@link Notification javadoc}:
	 * <p><i>"It is strongly recommended that notification senders use the object name
	 * rather than a reference to the MBean object as the source."</i>
	 * @param notification the {@link Notification} whose
	 * {@link javax.management.Notification#getSource()} might need massaging
	 */
	private void replaceNotificationSourceIfNecessary(Notification notification) {
		if (notification.getSource() == null || notification.getSource().equals(this.managedResource)) {
			notification.setSource(this.objectName);
		}
	}

}
