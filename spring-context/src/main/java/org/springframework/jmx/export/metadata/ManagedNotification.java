/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.jmx.export.metadata;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * Metadata that indicates a JMX notification emitted by a bean.
 *
 * @author Rob Harrop
 * @since 2.0
 */
public class ManagedNotification {

	@Nullable
	private String[] notificationTypes;

	@Nullable
	private String name;

	@Nullable
	private String description;


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
	public void setNotificationTypes(@Nullable String... notificationTypes) {
		this.notificationTypes = notificationTypes;
	}

	/**
	 * Return the list of notification types.
	 */
	@Nullable
	public String[] getNotificationTypes() {
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
	@Nullable
	public String getName() {
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
	@Nullable
	public String getDescription() {
		return this.description;
	}

}
