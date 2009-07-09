/*
 * Copyright 2004-2009 the original author or authors.
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
package org.springframework.ui.binding.support;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.GenericTypeResolver;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.ui.format.AnnotationFormatterFactory;
import org.springframework.ui.format.Formatted;
import org.springframework.ui.format.Formatter;

/**
 * A generic implementation of {@link FormatterRegistry} suitable for use in most binding environments.
 * @author Keith Donald
 * @since 3.0
 * @see #add(Class, Formatter)
 * @see #add(AnnotationFormatterFactory)
 */
@SuppressWarnings("unchecked")
public class GenericFormatterRegistry implements FormatterRegistry {

	private Map<Class, Formatter> typeFormatters = new ConcurrentHashMap<Class, Formatter>();

	private Map<GenericCollectionPropertyType, Formatter> collectionTypeFormatters = new ConcurrentHashMap<GenericCollectionPropertyType, Formatter>();

	private Map<Class, AnnotationFormatterFactory> annotationFormatters = new HashMap<Class, AnnotationFormatterFactory>();

	public Formatter<?> getFormatter(TypeDescriptor<?> propertyType) {
		Annotation[] annotations = propertyType.getAnnotations();
		for (Annotation a : annotations) {
			AnnotationFormatterFactory factory = annotationFormatters.get(a.annotationType());
			if (factory != null) {
				return factory.getFormatter(a);
			}
		}
		Formatter<?> formatter = null;
		Class<?> type;
		if (propertyType.isCollection()) {
			formatter = collectionTypeFormatters.get(new GenericCollectionPropertyType(propertyType.getType(), propertyType.getElementType()));
			if (formatter != null) {
				return formatter;
			} else {
				type = propertyType.getElementType();
			}
		} else {
			type = propertyType.getType();
		}
		formatter = typeFormatters.get(type);
		if (formatter != null) {
			return formatter;
		} else {
			Formatted formatted = AnnotationUtils.findAnnotation(type, Formatted.class);
			if (formatted != null) {
				Class formatterClass = formatted.value();
				try {
					formatter = (Formatter) formatterClass.newInstance();
				} catch (InstantiationException e) {
					// TODO better runtime exception
					throw new IllegalStateException(e);
				} catch (IllegalAccessException e) {
					throw new IllegalStateException(e);
				}
				typeFormatters.put(type, formatter);
				return formatter;
			} else {
				return null;
			}
		}
	}

	public void add(Class<?> propertyType, Formatter<?> formatter) {
		if (propertyType.isAnnotation()) {
			annotationFormatters.put(propertyType, new SimpleAnnotationFormatterFactory(formatter));
		} else {
			typeFormatters.put(propertyType, formatter);
		}
	}

	public void add(GenericCollectionPropertyType propertyType, Formatter<?> formatter) {
		collectionTypeFormatters.put(propertyType, formatter);
	}
	
	public void add(AnnotationFormatterFactory<?, ?> factory) {
		annotationFormatters.put(getAnnotationType(factory), factory);
	}

	// internal helpers
	
	private Class getAnnotationType(AnnotationFormatterFactory factory) {
		Class classToIntrospect = factory.getClass();
		while (classToIntrospect != null) {
			Type[] genericInterfaces = classToIntrospect.getGenericInterfaces();
			for (Type genericInterface : genericInterfaces) {
				if (genericInterface instanceof ParameterizedType) {
					ParameterizedType pInterface = (ParameterizedType) genericInterface;
					if (AnnotationFormatterFactory.class.isAssignableFrom((Class) pInterface.getRawType())) {
						return getParameterClass(pInterface.getActualTypeArguments()[0], factory.getClass());
					}
				}
			}
			classToIntrospect = classToIntrospect.getSuperclass();
		}
		throw new IllegalArgumentException(
				"Unable to extract Annotation type A argument from AnnotationFormatterFactory ["
						+ factory.getClass().getName() + "]; does the factory parameterize the <A> generic type?");
	}
	
	private Class getParameterClass(Type parameterType, Class converterClass) {
		if (parameterType instanceof TypeVariable) {
			parameterType = GenericTypeResolver.resolveTypeVariable((TypeVariable) parameterType, converterClass);
		}
		if (parameterType instanceof Class) {
			return (Class) parameterType;
		}
		throw new IllegalArgumentException("Unable to obtain the java.lang.Class for parameterType [" + parameterType
				+ "] on Formatter [" + converterClass.getName() + "]");
	}
	
	static class SimpleAnnotationFormatterFactory implements AnnotationFormatterFactory {

		private Formatter formatter;

		public SimpleAnnotationFormatterFactory(Formatter formatter) {
			this.formatter = formatter;
		}

		public Formatter getFormatter(Annotation annotation) {
			return formatter;
		}

	}

}
