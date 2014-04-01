/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.core.annotation;

import java.lang.annotation.Annotation;

import org.springframework.util.ClassUtils;

/**
 * General utility for determining the order of an object based
 * on its type declaration.
 *
 * @author Stephane Nicoll
 * @since 4.1
 * @see Order
 * @see javax.annotation.Priority
 */
public abstract class OrderUtils {

	private static final String PRIORITY_ANNOTATION_CLASS_NAME = "javax.annotation.Priority";

	private static final boolean priorityPresent =
			ClassUtils.isPresent(PRIORITY_ANNOTATION_CLASS_NAME, OrderUtils.class.getClassLoader());

	/**
	 * Return the order on the specified {@code type} or the specified
	 * default value if none can be found.
	 * <p>Take care of {@link Order @Order} and {@code @javax.annotation.Priority}.
	 * @param type the type to handle
	 * @return the priority value of the default if none can be found
	 */
	public static Integer getOrder(Class<?> type, Integer defaultOrder) {
		Order order = AnnotationUtils.findAnnotation(type, Order.class);
		if (order != null) {
			return order.value();
		}
		Integer priorityOrder = getPriorityValue(type);
		if (priorityOrder != null) {
			return priorityOrder;
		}
		return defaultOrder;
	}

	/**
	 * Return the value of the {@code javax.annotation.Priority} annotation set on the
	 * specified type or {@code null} if none is set.
	 * @param type the type to handle
	 * @return the priority value if the annotation is set, {@code null} otherwise
	 */
	public static Integer getPriorityValue(Class<?> type) {
		if (priorityPresent) {
			for (Annotation annotation : type.getAnnotations()) {
				if (PRIORITY_ANNOTATION_CLASS_NAME.equals(annotation.annotationType().getName())) {
					return (Integer) AnnotationUtils.getValue(annotation);
				}
			}
		}
		return null;
	}

}
