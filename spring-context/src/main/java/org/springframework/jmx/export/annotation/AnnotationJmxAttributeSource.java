/*
 * Copyright 2002-2016 the original author or authors.
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

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Set;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.annotation.AnnotationBeanUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.EmbeddedValueResolver;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.jmx.export.metadata.InvalidMetadataException;
import org.springframework.jmx.export.metadata.JmxAttributeSource;
import org.springframework.util.StringValueResolver;

/**
 * Implementation of the {@code JmxAttributeSource} interface that
 * reads annotations and exposes the corresponding attributes.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Jennifer Hickey
 * @author Stephane Nicoll
 * @since 1.2
 * @see ManagedResource
 * @see ManagedAttribute
 * @see ManagedOperation
 */
public class AnnotationJmxAttributeSource implements JmxAttributeSource, BeanFactoryAware {

	private StringValueResolver embeddedValueResolver;


	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		if (beanFactory instanceof ConfigurableBeanFactory) {
			this.embeddedValueResolver = new EmbeddedValueResolver((ConfigurableBeanFactory) beanFactory);
		}
	}


	@Override
	public org.springframework.jmx.export.metadata.ManagedResource getManagedResource(Class<?> beanClass) throws InvalidMetadataException {
		ManagedResource ann = AnnotationUtils.findAnnotation(beanClass, ManagedResource.class);
		if (ann == null) {
			return null;
		}
		Class<?> declaringClass = AnnotationUtils.findAnnotationDeclaringClass(ManagedResource.class, beanClass);
		Class<?> target = (declaringClass != null && !declaringClass.isInterface() ? declaringClass : beanClass);
		if (!Modifier.isPublic(target.getModifiers())) {
			throw new InvalidMetadataException("@ManagedResource class '" + target.getName() + "' must be public");
		}
		org.springframework.jmx.export.metadata.ManagedResource managedResource = new org.springframework.jmx.export.metadata.ManagedResource();
		AnnotationBeanUtils.copyPropertiesToBean(ann, managedResource, this.embeddedValueResolver);
		return managedResource;
	}

	@Override
	public org.springframework.jmx.export.metadata.ManagedAttribute getManagedAttribute(Method method) throws InvalidMetadataException {
		ManagedAttribute ann = AnnotationUtils.findAnnotation(method, ManagedAttribute.class);
		if (ann == null) {
			return null;
		}
		org.springframework.jmx.export.metadata.ManagedAttribute managedAttribute = new org.springframework.jmx.export.metadata.ManagedAttribute();
		AnnotationBeanUtils.copyPropertiesToBean(ann, managedAttribute, "defaultValue");
		if (ann.defaultValue().length() > 0) {
			managedAttribute.setDefaultValue(ann.defaultValue());
		}
		return managedAttribute;
	}

	@Override
	public org.springframework.jmx.export.metadata.ManagedMetric getManagedMetric(Method method) throws InvalidMetadataException {
		ManagedMetric ann = AnnotationUtils.findAnnotation(method, ManagedMetric.class);
		return copyPropertiesToBean(ann, org.springframework.jmx.export.metadata.ManagedMetric.class);
	}

	@Override
	public org.springframework.jmx.export.metadata.ManagedOperation getManagedOperation(Method method) throws InvalidMetadataException {
		ManagedOperation ann = AnnotationUtils.findAnnotation(method, ManagedOperation.class);
		return copyPropertiesToBean(ann, org.springframework.jmx.export.metadata.ManagedOperation.class);
	}

	@Override
	public org.springframework.jmx.export.metadata.ManagedOperationParameter[] getManagedOperationParameters(Method method)
			throws InvalidMetadataException {

		Set<ManagedOperationParameter> anns = AnnotationUtils.getRepeatableAnnotations(
				method, ManagedOperationParameter.class, ManagedOperationParameters.class);
		return copyPropertiesToBeanArray(anns, org.springframework.jmx.export.metadata.ManagedOperationParameter.class);
	}

	@Override
	public org.springframework.jmx.export.metadata.ManagedNotification[] getManagedNotifications(Class<?> clazz)
			throws InvalidMetadataException {

		Set<ManagedNotification> anns = AnnotationUtils.getRepeatableAnnotations(
				clazz, ManagedNotification.class, ManagedNotifications.class);
		return copyPropertiesToBeanArray(anns, org.springframework.jmx.export.metadata.ManagedNotification.class);
	}


	@SuppressWarnings("unchecked")
	private static <T> T[] copyPropertiesToBeanArray(Collection<? extends Annotation> anns, Class<T> beanClass) {
		T[] beans = (T[]) Array.newInstance(beanClass, anns.size());
		int i = 0;
		for (Annotation ann : anns) {
			beans[i++] = copyPropertiesToBean(ann, beanClass);
		}
		return beans;
	}

	private static <T> T copyPropertiesToBean(Annotation ann, Class<T> beanClass) {
		if (ann == null) {
			return null;
		}
		T bean = BeanUtils.instantiateClass(beanClass);
		AnnotationBeanUtils.copyPropertiesToBean(ann, bean);
		return bean;
	}

}
