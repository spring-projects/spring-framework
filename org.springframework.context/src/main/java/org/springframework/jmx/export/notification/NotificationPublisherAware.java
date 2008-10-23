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

/**
 * Interface to be implemented by any Spring-managed resource that is to be
 * registered with an {@link javax.management.MBeanServer} and wishes to send
 * JMX {@link javax.management.Notification javax.management.Notifications}.
 *
 * <p>Provides Spring-created managed resources with a {@link NotificationPublisher}
 * as soon as they are registered with the {@link javax.management.MBeanServer}.
 *
 * <p><b>NOTE:</b> This interface only applies to simple Spring-managed
 * beans which happen to get exported through Spring's
 * {@link org.springframework.jmx.export.MBeanExporter}.
 * It does not apply to any non-exported beans; neither does it apply
 * to standard MBeans exported by Spring. For standard JMX MBeans,
 * consider implementing the {@link javax.management.modelmbean.ModelMBeanNotificationBroadcaster}
 * interface (or implementing a full {@link javax.management.modelmbean.ModelMBean}).
 *
 * @author Rob Harrop
 * @since 2.0
 * @see NotificationPublisher
 */
public interface NotificationPublisherAware {

	/**
	 * Set the {@link NotificationPublisher} instance for the current managed resource instance.
	 */
	void setNotificationPublisher(NotificationPublisher notificationPublisher);

}
