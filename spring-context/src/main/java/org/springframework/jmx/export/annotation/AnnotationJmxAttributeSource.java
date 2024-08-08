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

package org.springframework.jmx.export.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.EmbeddedValueResolver;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotationPredicates;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.core.annotation.RepeatableContainers;
import org.springframework.jmx.export.metadata.InvalidMetadataException;
import org.springframework.jmx.export.metadata.JmxAttributeSource;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;

/**
 * Implementation of the {@link JmxAttributeSource} interface that
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

	@Nullable
	private StringValueResolver embeddedValueResolver;


	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		if (beanFactory instanceof ConfigurableBeanFactory cbf) {
			this.embeddedValueResolver = new EmbeddedValueResolver(cbf);
		}
	}


	@Override
	@Nullable
	public org.springframework.jmx.export.metadata.ManagedResource getManagedResource(Class<?> beanClass) throws InvalidMetadataException {
		MergedAnnotation<ManagedResource> ann = MergedAnnotations.from(beanClass, SearchStrategy.TYPE_HIERARCHY)
				.get(ManagedResource.class).withNonMergedAttributes();
		if (!ann.isPresent()) {
			return null;
		}
		Class<?> declaringClass = (Class<?>) ann.getSource();
		Class<?> target = (declaringClass != null && !declaringClass.isInterface() ? declaringClass : beanClass);
		if (!Modifier.isPublic(target.getModifiers())) {
			throw new InvalidMetadataException("@ManagedResource class '" + target.getName() + "' must be public");
		}

		org.springframework.jmx.export.metadata.ManagedResource bean = new org.springframework.jmx.export.metadata.ManagedResource();
		Map<String, Object> map = ann.asMap();
		List<PropertyValue> list = new ArrayList<>(map.size());
		map.forEach((attrName, attrValue) -> {
			if (!"value".equals(attrName)) {
				Object value = attrValue;
				if (this.embeddedValueResolver != null && value instanceof String text) {
					value = this.embeddedValueResolver.resolveStringValue(text);
				}
				list.add(new PropertyValue(attrName, value));
			}
		});
		PropertyAccessorFactory.forBeanPropertyAccess(bean).setPropertyValues(new MutablePropertyValues(list));
		return bean;
	}

	@Override
	@Nullable
	public org.springframework.jmx.export.metadata.ManagedAttribute getManagedAttribute(Method method) throws InvalidMetadataException {
		MergedAnnotation<ManagedAttribute> ann = MergedAnnotations.from(method, SearchStrategy.TYPE_HIERARCHY)
				.get(ManagedAttribute.class).withNonMergedAttributes();
		if (!ann.isPresent()) {
			return null;
		}

		org.springframework.jmx.export.metadata.ManagedAttribute bean = new org.springframework.jmx.export.metadata.ManagedAttribute();
		Map<String, Object> map = ann.asMap();
		MutablePropertyValues pvs = new MutablePropertyValues(map);
		pvs.removePropertyValue("defaultValue");
		PropertyAccessorFactory.forBeanPropertyAccess(bean).setPropertyValues(pvs);
		String defaultValue = (String) map.get("defaultValue");
		if (StringUtils.hasLength(defaultValue)) {
			bean.setDefaultValue(defaultValue);
		}
		return bean;
	}

	@Override
	@Nullable
	public org.springframework.jmx.export.metadata.ManagedMetric getManagedMetric(Method method) throws InvalidMetadataException {
		MergedAnnotation<ManagedMetric> ann = MergedAnnotations.from(method, SearchStrategy.TYPE_HIERARCHY)
				.get(ManagedMetric.class).withNonMergedAttributes();

		return copyPropertiesToBean(ann, org.springframework.jmx.export.metadata.ManagedMetric.class);
	}

	@Override
	@Nullable
	public org.springframework.jmx.export.metadata.ManagedOperation getManagedOperation(Method method) throws InvalidMetadataException {
		MergedAnnotation<ManagedOperation> ann = MergedAnnotations.from(method, SearchStrategy.TYPE_HIERARCHY)
				.get(ManagedOperation.class).withNonMergedAttributes();

		return copyPropertiesToBean(ann, org.springframework.jmx.export.metadata.ManagedOperation.class);
	}

	@Override
	public org.springframework.jmx.export.metadata.ManagedOperationParameter[] getManagedOperationParameters(Method method)
			throws InvalidMetadataException {

		List<MergedAnnotation<? extends Annotation>> anns = getRepeatableAnnotations(
				method, ManagedOperationParameter.class, ManagedOperationParameters.class);

		return copyPropertiesToBeanArray(anns, org.springframework.jmx.export.metadata.ManagedOperationParameter.class);
	}

	@Override
	public org.springframework.jmx.export.metadata.ManagedNotification[] getManagedNotifications(Class<?> clazz)
			throws InvalidMetadataException {

		List<MergedAnnotation<? extends Annotation>> anns = getRepeatableAnnotations(
				clazz, ManagedNotification.class, ManagedNotifications.class);

		return copyPropertiesToBeanArray(anns, org.springframework.jmx.export.metadata.ManagedNotification.class);
	}


	private static List<MergedAnnotation<? extends Annotation>> getRepeatableAnnotations(
			AnnotatedElement annotatedElement, Class<? extends Annotation> annotationType,
			Class<? extends Annotation> containerAnnotationType) {

		return MergedAnnotations.from(annotatedElement, SearchStrategy.TYPE_HIERARCHY,
				RepeatableContainers.of(annotationType, containerAnnotationType))
				.stream(annotationType)
				.filter(MergedAnnotationPredicates.firstRunOf(MergedAnnotation::getAggregateIndex))
				.map(MergedAnnotation::withNonMergedAttributes)
				.collect(Collectors.toList());
	}

	@SuppressWarnings("unchecked")
	private static <T> T[] copyPropertiesToBeanArray(
			List<MergedAnnotation<? extends Annotation>> anns, Class<T> beanClass) {

		T[] beans = (T[]) Array.newInstance(beanClass, anns.size());
		int i = 0;
		for (MergedAnnotation<? extends Annotation> ann : anns) {
			beans[i++] = copyPropertiesToBean(ann, beanClass);
		}
		return beans;
	}

	@Nullable
	private static <T> T copyPropertiesToBean(MergedAnnotation<? extends Annotation> ann, Class<T> beanClass) {
		if (!ann.isPresent()) {
			return null;
		}
		T bean = BeanUtils.instantiateClass(beanClass);
		BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(bean);
		bw.setPropertyValues(new MutablePropertyValues(ann.asMap()));
		return bean;
	}

}
