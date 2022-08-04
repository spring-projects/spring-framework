/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.beans.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringValueResolver;

/**
 * General utility methods for working with annotations in JavaBeans style.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 * @deprecated as of 5.2, in favor of custom annotation attribute processing
 */
@Deprecated
public abstract class AnnotationBeanUtils {

	/**
	 * Copy the properties of the supplied {@link Annotation} to the supplied target bean.
	 * Any properties defined in {@code excludedProperties} will not be copied.
	 * @param ann the annotation to copy from
	 * @param bean the bean instance to copy to
	 * @param excludedProperties the names of excluded properties, if any
	 * @see org.springframework.beans.BeanWrapper
	 */
	public static void copyPropertiesToBean(Annotation ann, Object bean, String... excludedProperties) {
		copyPropertiesToBean(ann, bean, null, excludedProperties);
	}

	/**
	 * Copy the properties of the supplied {@link Annotation} to the supplied target bean.
	 * Any properties defined in {@code excludedProperties} will not be copied.
	 * <p>A specified value resolver may resolve placeholders in property values, for example.
	 * @param ann the annotation to copy from
	 * @param bean the bean instance to copy to
	 * @param valueResolver a resolve to post-process String property values (may be {@code null})
	 * @param excludedProperties the names of excluded properties, if any
	 * @see org.springframework.beans.BeanWrapper
	 */
	public static void copyPropertiesToBean(Annotation ann, Object bean, @Nullable StringValueResolver valueResolver,
			String... excludedProperties) {

		Set<String> excluded = (excludedProperties.length == 0 ? Collections.emptySet() :
				new HashSet<>(Arrays.asList(excludedProperties)));
		Method[] annotationProperties = ann.annotationType().getDeclaredMethods();
		BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(bean);
		for (Method annotationProperty : annotationProperties) {
			String propertyName = annotationProperty.getName();
			if (!excluded.contains(propertyName) && bw.isWritableProperty(propertyName)) {
				Object value = ReflectionUtils.invokeMethod(annotationProperty, ann);
				if (valueResolver != null && value instanceof String) {
					value = valueResolver.resolveStringValue((String) value);
				}
				bw.setPropertyValue(propertyName, value);
			}
		}
	}

}
