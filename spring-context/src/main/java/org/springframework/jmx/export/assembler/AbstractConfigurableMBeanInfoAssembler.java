/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.jmx.export.assembler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.management.modelmbean.ModelMBeanNotificationInfo;

import org.springframework.jmx.export.metadata.JmxMetadataUtils;
import org.springframework.jmx.export.metadata.ManagedNotification;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * Base class for MBeanInfoAssemblers that support configurable
 * JMX notification behavior.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
public abstract class AbstractConfigurableMBeanInfoAssembler extends AbstractReflectiveMBeanInfoAssembler {

	@Nullable
	private ModelMBeanNotificationInfo[] notificationInfos;

	private final Map<String, ModelMBeanNotificationInfo[]> notificationInfoMappings = new HashMap<>();


	public void setNotificationInfos(ManagedNotification[] notificationInfos) {
		ModelMBeanNotificationInfo[] infos = new ModelMBeanNotificationInfo[notificationInfos.length];
		for (int i = 0; i < notificationInfos.length; i++) {
			ManagedNotification notificationInfo = notificationInfos[i];
			infos[i] = JmxMetadataUtils.convertToModelMBeanNotificationInfo(notificationInfo);
		}
		this.notificationInfos = infos;
	}

	public void setNotificationInfoMappings(Map<String, Object> notificationInfoMappings) {
		notificationInfoMappings.forEach((beanKey, result) ->
				this.notificationInfoMappings.put(beanKey, extractNotificationMetadata(result)));
	}


	@Override
	protected ModelMBeanNotificationInfo[] getNotificationInfo(Object managedBean, String beanKey) {
		ModelMBeanNotificationInfo[] result = null;
		if (StringUtils.hasText(beanKey)) {
			result = this.notificationInfoMappings.get(beanKey);
		}
		if (result == null) {
			result = this.notificationInfos;
		}
		return (result != null ? result : new ModelMBeanNotificationInfo[0]);
	}

	private ModelMBeanNotificationInfo[] extractNotificationMetadata(Object mapValue) {
		if (mapValue instanceof ManagedNotification) {
			ManagedNotification mn = (ManagedNotification) mapValue;
			return new ModelMBeanNotificationInfo[] {JmxMetadataUtils.convertToModelMBeanNotificationInfo(mn)};
		}
		else if (mapValue instanceof Collection) {
			Collection<?> col = (Collection<?>) mapValue;
			List<ModelMBeanNotificationInfo> result = new ArrayList<>();
			for (Object colValue : col) {
				if (!(colValue instanceof ManagedNotification)) {
					throw new IllegalArgumentException(
							"Property 'notificationInfoMappings' only accepts ManagedNotifications for Map values");
				}
				ManagedNotification mn = (ManagedNotification) colValue;
				result.add(JmxMetadataUtils.convertToModelMBeanNotificationInfo(mn));
			}
			return result.toArray(new ModelMBeanNotificationInfo[0]);
		}
		else {
			throw new IllegalArgumentException(
					"Property 'notificationInfoMappings' only accepts ManagedNotifications for Map values");
		}
	}

}
