/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jmx.export.metadata;

import org.jspecify.annotations.Nullable;

import org.springframework.util.StringUtils;

/**
 * Metadata that indicates a JMX notification emitted by a bean.
 *
 * @author Rob Harrop
 * @since 2.0
 */
public class ManagedNotification {

	private String @Nullable [] notificationTypes;

	private @Nullable String name;

	private @Nullable String description;


	/**
	 * Set a single notification type, or a list of notification types
	 * as comma-delimited String.
	 */
	public void setNotificationType(String notificationType) {
		this.notificationTypes = StringUtils.commaDelimitedListToStringArray(notificationType);
	}

	/**
	 * Set a list of notification types.
	 */
	public void setNotificationTypes(String @Nullable ... notificationTypes) {
		this.notificationTypes = notificationTypes;
	}

	/**
	 * Return the list of notification types.
	 */
	public String @Nullable [] getNotificationTypes() {
		return this.notificationTypes;
	}

	/**
	 * Set the name of this notification.
	 */
	public void setName(@Nullable String name) {
		this.name = name;
	}

	/**
	 * Return the name of this notification.
	 */
	public @Nullable String getName() {
		return this.name;
	}

	/**
	 * Set a description for this notification.
	 */
	public void setDescription(@Nullable String description) {
		this.description = description;
	}

	/**
	 * Return a description for this notification.
	 */
	public @Nullable String getDescription() {
		return this.description;
	}

}
