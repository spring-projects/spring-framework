/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jmx.export.metadata;

import javax.management.modelmbean.ModelMBeanNotificationInfo;

import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Utility methods for converting Spring JMX metadata into their plain JMX equivalents.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
public abstract class JmxMetadataUtils {

	/**
	 * Convert the supplied {@link ManagedNotification} into the corresponding
	 * {@link javax.management.modelmbean.ModelMBeanNotificationInfo}.
	 */
	public static ModelMBeanNotificationInfo convertToModelMBeanNotificationInfo(ManagedNotification notificationInfo) {
		String[] notifTypes = notificationInfo.getNotificationTypes();
		if (ObjectUtils.isEmpty(notifTypes)) {
			throw new IllegalArgumentException("Must specify at least one notification type");
		}

		String name = notificationInfo.getName();
		if (!StringUtils.hasText(name)) {
			throw new IllegalArgumentException("Must specify notification name");
		}

		String description = notificationInfo.getDescription();
		return new ModelMBeanNotificationInfo(notifTypes, name, description);
	}

}
