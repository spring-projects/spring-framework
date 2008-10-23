/*
 * Copyright 2002-2008 the original author or authors.
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

import javax.management.NotificationListener;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.jmx.support.NotificationListenerHolder;

/**
 * Helper class that aggregates a {@link javax.management.NotificationListener},
 * a {@link javax.management.NotificationFilter}, and an arbitrary handback
 * object.
 *
 * <p>Also provides support for associating the encapsulated
 * {@link javax.management.NotificationListener} with any number of
 * MBeans from which it wishes to receive
 * {@link javax.management.Notification Notifications} via the
 * {@link #setMappedObjectNames mappedObjectNames} property.
 *
 * <p>Note: This class supports Spring bean names as
 * {@link #setMappedObjectNames "mappedObjectNames"} as well, as alternative
 * to specifying JMX object names. Note that only beans exported by the
 * same {@link MBeanExporter} are supported for such bean names.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 * @see MBeanExporter#setNotificationListeners
 */
public class NotificationListenerBean extends NotificationListenerHolder implements InitializingBean {

	/**
	 * Create a new instance of the {@link NotificationListenerBean} class.
	 */
	public NotificationListenerBean() {
	}

	/**
	 * Create a new instance of the {@link NotificationListenerBean} class.
	 * @param notificationListener the encapsulated listener
	 */
	public NotificationListenerBean(NotificationListener notificationListener) {
		setNotificationListener(notificationListener);
	}


	public void afterPropertiesSet() {
		if (getNotificationListener() == null) {
			throw new IllegalArgumentException("Property 'notificationListener' is required");
		}
	}

	void replaceObjectName(Object originalName, Object newName) {
		if (this.mappedObjectNames != null && this.mappedObjectNames.contains(originalName)) {
			this.mappedObjectNames.remove(originalName);
			this.mappedObjectNames.add(newName);
		}
	}

}
