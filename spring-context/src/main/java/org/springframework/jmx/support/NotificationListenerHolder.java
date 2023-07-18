/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.jmx.support;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.management.MalformedObjectNameException;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

/**
 * Helper class that aggregates a {@link javax.management.NotificationListener},
 * a {@link javax.management.NotificationFilter}, and an arbitrary handback
 * object, as well as the names of MBeans from which the listener wishes
 * to receive {@link javax.management.Notification Notifications}.
 *
 * @author Juergen Hoeller
 * @since 2.5.2
 * @see org.springframework.jmx.export.NotificationListenerBean
 * @see org.springframework.jmx.access.NotificationListenerRegistrar
 */
public class NotificationListenerHolder {

	@Nullable
	private NotificationListener notificationListener;

	@Nullable
	private NotificationFilter notificationFilter;

	@Nullable
	private Object handback;

	@Nullable
	protected Set<Object> mappedObjectNames;


	/**
	 * Set the {@link javax.management.NotificationListener}.
	 */
	public void setNotificationListener(@Nullable NotificationListener notificationListener) {
		this.notificationListener = notificationListener;
	}

	/**
	 * Get the {@link javax.management.NotificationListener}.
	 */
	@Nullable
	public NotificationListener getNotificationListener() {
		return this.notificationListener;
	}

	/**
	 * Set the {@link javax.management.NotificationFilter} associated
	 * with the encapsulated {@link #getNotificationFilter() NotificationFilter}.
	 * <p>May be {@code null}.
	 */
	public void setNotificationFilter(@Nullable NotificationFilter notificationFilter) {
		this.notificationFilter = notificationFilter;
	}

	/**
	 * Return the {@link javax.management.NotificationFilter} associated
	 * with the encapsulated {@link #getNotificationListener() NotificationListener}.
	 * <p>May be {@code null}.
	 */
	@Nullable
	public NotificationFilter getNotificationFilter() {
		return this.notificationFilter;
	}

	/**
	 * Set the (arbitrary) object that will be 'handed back' as-is by an
	 * {@link javax.management.NotificationBroadcaster} when notifying
	 * any {@link javax.management.NotificationListener}.
	 * @param handback the handback object (can be {@code null})
	 * @see javax.management.NotificationListener#handleNotification(javax.management.Notification, Object)
	 */
	public void setHandback(@Nullable Object handback) {
		this.handback = handback;
	}

	/**
	 * Return the (arbitrary) object that will be 'handed back' as-is by an
	 * {@link javax.management.NotificationBroadcaster} when notifying
	 * any {@link javax.management.NotificationListener}.
	 * @return the handback object (may be {@code null})
	 * @see javax.management.NotificationListener#handleNotification(javax.management.Notification, Object)
	 */
	@Nullable
	public Object getHandback() {
		return this.handback;
	}

	/**
	 * Set the {@link javax.management.ObjectName}-style name of the single MBean
	 * that the encapsulated {@link #getNotificationFilter() NotificationFilter}
	 * will be registered with to listen for {@link javax.management.Notification Notifications}.
	 * Can be specified as {@code ObjectName} instance or as {@code String}.
	 * @see #setMappedObjectNames
	 */
	public void setMappedObjectName(@Nullable Object mappedObjectName) {
		this.mappedObjectNames = (mappedObjectName != null ?
				new LinkedHashSet<>(Collections.singleton(mappedObjectName)) : null);
	}

	/**
	 * Set an array of {@link javax.management.ObjectName}-style names of the MBeans
	 * that the encapsulated {@link #getNotificationFilter() NotificationFilter}
	 * will be registered with to listen for {@link javax.management.Notification Notifications}.
	 * Can be specified as {@code ObjectName} instances or as {@code String}s.
	 * @see #setMappedObjectName
	 */
	public void setMappedObjectNames(Object... mappedObjectNames) {
		this.mappedObjectNames = new LinkedHashSet<>(Arrays.asList(mappedObjectNames));
	}

	/**
	 * Return the list of {@link javax.management.ObjectName} String representations for
	 * which the encapsulated {@link #getNotificationFilter() NotificationFilter} will
	 * be registered as a listener for {@link javax.management.Notification Notifications}.
	 * @throws MalformedObjectNameException if an {@code ObjectName} is malformed
	 */
	@Nullable
	public ObjectName[] getResolvedObjectNames() throws MalformedObjectNameException {
		if (this.mappedObjectNames == null) {
			return null;
		}
		ObjectName[] resolved = new ObjectName[this.mappedObjectNames.size()];
		int i = 0;
		for (Object objectName : this.mappedObjectNames) {
			resolved[i] = ObjectNameManager.getInstance(objectName);
			i++;
		}
		return resolved;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof NotificationListenerHolder that &&
				ObjectUtils.nullSafeEquals(this.notificationListener, that.notificationListener) &&
				ObjectUtils.nullSafeEquals(this.notificationFilter, that.notificationFilter) &&
				ObjectUtils.nullSafeEquals(this.handback, that.handback) &&
				ObjectUtils.nullSafeEquals(this.mappedObjectNames, that.mappedObjectNames)));
	}

	@Override
	public int hashCode() {
		int hashCode = ObjectUtils.nullSafeHashCode(this.notificationListener);
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.notificationFilter);
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.handback);
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.mappedObjectNames);
		return hashCode;
	}

}
