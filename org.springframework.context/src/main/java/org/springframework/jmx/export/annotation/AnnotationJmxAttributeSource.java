/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.jmx.export.annotation;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.annotation.AnnotationBeanUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.jmx.export.metadata.InvalidMetadataException;
import org.springframework.jmx.export.metadata.JmxAttributeSource;
import org.springframework.jmx.export.metadata.ManagedAttribute;
import org.springframework.jmx.export.metadata.ManagedMetric;
import org.springframework.jmx.export.metadata.ManagedNotification;
import org.springframework.jmx.export.metadata.ManagedOperation;
import org.springframework.jmx.export.metadata.ManagedOperationParameter;
import org.springframework.jmx.export.metadata.ManagedResource;
import org.springframework.util.StringUtils;

/**
 * Implementation of the <code>JmxAttributeSource</code> interface that
 * reads JDK 1.5+ annotations and exposes the corresponding attributes.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Jennifer Hickey
 * @since 1.2
 * @see org.springframework.jmx.export.annotation.ManagedResource
 * @see org.springframework.jmx.export.annotation.ManagedAttribute
 * @see org.springframework.jmx.export.annotation.ManagedOperation
 */
public class AnnotationJmxAttributeSource implements JmxAttributeSource {

	public ManagedResource getManagedResource(Class<?> beanClass) throws InvalidMetadataException {
		org.springframework.jmx.export.annotation.ManagedResource ann =
				beanClass.getAnnotation(org.springframework.jmx.export.annotation.ManagedResource.class);
		if (ann == null) {
			return null;
		}
		ManagedResource managedResource = new ManagedResource();
		AnnotationBeanUtils.copyPropertiesToBean(ann, managedResource);
		if (!"".equals(ann.value()) && !StringUtils.hasLength(managedResource.getObjectName())) {
			managedResource.setObjectName(ann.value());
		}
		return managedResource;
	}

	public ManagedAttribute getManagedAttribute(Method method) throws InvalidMetadataException {
		org.springframework.jmx.export.annotation.ManagedAttribute ann =
				AnnotationUtils.findAnnotation(method, org.springframework.jmx.export.annotation.ManagedAttribute.class);
		if (ann == null) {
			return null;
		}
		ManagedAttribute managedAttribute = new ManagedAttribute();
		AnnotationBeanUtils.copyPropertiesToBean(ann, managedAttribute, "defaultValue");
		if (ann.defaultValue().length() > 0) {
			managedAttribute.setDefaultValue(ann.defaultValue());
		}
		return managedAttribute;
	}

	public ManagedMetric getManagedMetric(Method method) throws InvalidMetadataException {
		org.springframework.jmx.export.annotation.ManagedMetric ann =
				AnnotationUtils.findAnnotation(method, org.springframework.jmx.export.annotation.ManagedMetric.class);
		if (ann == null) {
			return null;
		}
		ManagedMetric managedMetric = new ManagedMetric();
		AnnotationBeanUtils.copyPropertiesToBean(ann, managedMetric);
		return managedMetric;
	}

	public ManagedOperation getManagedOperation(Method method) throws InvalidMetadataException {
		Annotation ann = AnnotationUtils.findAnnotation(method, org.springframework.jmx.export.annotation.ManagedOperation.class);
		if (ann == null) {
			return null;
		}
		ManagedOperation op = new ManagedOperation();
		AnnotationBeanUtils.copyPropertiesToBean(ann, op);
		return op;
	}

	public ManagedOperationParameter[] getManagedOperationParameters(Method method)
			throws InvalidMetadataException {

		ManagedOperationParameters params = AnnotationUtils.findAnnotation(method, ManagedOperationParameters.class);
		ManagedOperationParameter[] result = null;
		if (params == null) {
			result = new ManagedOperationParameter[0];
		}
		else {
			Annotation[] paramData = params.value();
			result = new ManagedOperationParameter[paramData.length];
			for (int i = 0; i < paramData.length; i++) {
				Annotation annotation = paramData[i];
				ManagedOperationParameter managedOperationParameter = new ManagedOperationParameter();
				AnnotationBeanUtils.copyPropertiesToBean(annotation, managedOperationParameter);
				result[i] = managedOperationParameter;
			}
		}
		return result;
	}

	public ManagedNotification[] getManagedNotifications(Class<?> clazz) throws InvalidMetadataException {
		ManagedNotifications notificationsAnn = clazz.getAnnotation(ManagedNotifications.class);
		if(notificationsAnn == null) {
			return new ManagedNotification[0];
		}
		Annotation[] notifications = notificationsAnn.value();
		ManagedNotification[] result = new ManagedNotification[notifications.length];
		for (int i = 0; i < notifications.length; i++) {
			Annotation notification = notifications[i];
			ManagedNotification managedNotification = new ManagedNotification();
			AnnotationBeanUtils.copyPropertiesToBean(notification, managedNotification);
			result[i] = managedNotification;
		}
		return result;
	}

}
