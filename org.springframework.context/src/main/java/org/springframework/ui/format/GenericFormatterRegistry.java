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
package org.springframework.ui.format;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.GenericTypeResolver;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.util.Assert;

/**
 * A generic implementation of {@link FormatterRegistry} suitable for use in most binding environments.
 * @author Keith Donald
 * @since 3.0
 * @see #add(Formatter)
 * @see #add(Class, Formatter)
 * @see #add(AnnotationFormatterFactory)
 */
@SuppressWarnings("unchecked")
public class GenericFormatterRegistry implements FormatterRegistry {

	private Map<Class, Formatter> typeFormatters = new ConcurrentHashMap<Class, Formatter>();

	private Map<Class, AnnotationFormatterFactory> annotationFormatters = new HashMap<Class, AnnotationFormatterFactory>();

	private ConversionService conversionService = new DefaultConversionService();

	/**
	 * Sets the type conversion service used to coerse objects to the types required for Formatting purposes.
	 * @param conversionService the conversion service
	 * @see #add(Class, Formatter)
	 */
	public void setConversionService(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	// implementing FormatterRegistry

	public <T> void add(Formatter<T> formatter) {
		typeFormatters.put(getFormattedObjectType(formatter.getClass()), formatter);
	}

	public <T> void add(Class<?> objectType, Formatter<T> formatter) {
		if (objectType.isAnnotation()) {
			annotationFormatters.put(objectType, new SimpleAnnotationFormatterFactory(formatter));
		} else {
			typeFormatters.put(objectType, formatter);
		}
	}

	public <A extends Annotation, T> void add(AnnotationFormatterFactory<A, T> factory) {
		annotationFormatters.put(getAnnotationType(factory.getClass()), factory);
	}

	public Formatter<?> getFormatter(TypeDescriptor type) {
		Assert.notNull(type, "The TypeDescriptor is required");
		Formatter formatter = getAnnotationFormatter(type);
		if (formatter == null) {
			formatter = getTypeFormatter(type.getType());
		}
		return formatter;
	}

	// internal helpers

	private Class getFormattedObjectType(Class formatterClass) {
		Class classToIntrospect = formatterClass;
		while (classToIntrospect != null) {
			Type[] ifcs = classToIntrospect.getGenericInterfaces();
			for (Type ifc : ifcs) {
				if (ifc instanceof ParameterizedType) {
					ParameterizedType paramIfc = (ParameterizedType) ifc;
					Type rawType = paramIfc.getRawType();
					if (Formatter.class.equals(rawType)) {
						Type arg = paramIfc.getActualTypeArguments()[0];
						if (arg instanceof TypeVariable) {
							arg = GenericTypeResolver.resolveTypeVariable((TypeVariable) arg, formatterClass);
						}
						if (arg instanceof Class) {
							return (Class) arg;
						}
					} else if (Formatter.class.isAssignableFrom((Class) rawType)) {
						return getFormattedObjectType((Class) rawType);
					}
				} else if (Formatter.class.isAssignableFrom((Class) ifc)) {
					return getFormattedObjectType((Class) ifc);
				}
			}
			classToIntrospect = classToIntrospect.getSuperclass();
		}
		return null;
	}

	private Class getAnnotationType(Class factoryClass) {
		Class classToIntrospect = factoryClass;
		while (classToIntrospect != null) {
			Type[] ifcs = classToIntrospect.getGenericInterfaces();
			for (Type ifc : ifcs) {
				if (ifc instanceof ParameterizedType) {
					ParameterizedType paramIfc = (ParameterizedType) ifc;
					Type rawType = paramIfc.getRawType();
					if (AnnotationFormatterFactory.class.equals(rawType)) {
						Type arg = paramIfc.getActualTypeArguments()[0];
						if (arg instanceof TypeVariable) {
							arg = GenericTypeResolver.resolveTypeVariable((TypeVariable) arg, factoryClass);
						}
						if (arg instanceof Class) {
							return (Class) arg;
						}
					} else if (AnnotationFormatterFactory.class.isAssignableFrom((Class) rawType)) {
						return getAnnotationType((Class) rawType);
					}
				} else if (AnnotationFormatterFactory.class.isAssignableFrom((Class) ifc)) {
					return getAnnotationType((Class) ifc);
				}
			}
			classToIntrospect = classToIntrospect.getSuperclass();
		}
		throw new IllegalArgumentException(
				"Unable to extract Annotation type A argument from AnnotationFormatterFactory ["
						+ factoryClass.getName() + "]; does the factory parameterize the <A> generic type?");
	}

	private Formatter<?> getAnnotationFormatter(TypeDescriptor type) {
		Annotation[] annotations = type.getAnnotations();
		for (Annotation a : annotations) {
			AnnotationFormatterFactory factory = annotationFormatters.get(a.annotationType());
			if (factory != null) {
				return factory.getFormatter(a);
			}
		}
		return null;
	}
	
	private Formatter<?> getTypeFormatter(Class<?> type) {
		Assert.notNull(type, "The Class of the object to format is required");
		Formatter formatter = typeFormatters.get(type);
		if (formatter != null) {
			Class<?> formattedObjectType = getFormattedObjectType(formatter.getClass());
			if (type.isAssignableFrom(formattedObjectType)) {
				return formatter;
			} else {
				return new ConvertingFormatter(type, formattedObjectType, formatter);
			}
		} else {
			return getDefaultFormatter(type);
		}
	}

	private Formatter<?> getDefaultFormatter(Class<?> type) {
		Formatted formatted = AnnotationUtils.findAnnotation(type, Formatted.class);
		if (formatted != null) {
			Class formatterClass = formatted.value();
			try {
				Formatter formatter = (Formatter) formatterClass.newInstance();
				typeFormatters.put(type, formatter);
				return formatter;
			} catch (InstantiationException e) {
				throw new IllegalStateException(
						"Formatter referenced by @Formatted annotation does not have default constructor", e);
			} catch (IllegalAccessException e) {
				throw new IllegalStateException(
						"Formatter referenced by @Formatted annotation does not have public constructor", e);
			}
		} else {
			return null;
		}
	}

	private static class SimpleAnnotationFormatterFactory implements AnnotationFormatterFactory {

		private Formatter formatter;

		public SimpleAnnotationFormatterFactory(Formatter formatter) {
			this.formatter = formatter;
		}

		public Formatter getFormatter(Annotation annotation) {
			return formatter;
		}

	}

	private class ConvertingFormatter implements Formatter {

		private Class<?> type;

		private Class<?> formattedObjectType;

		private Formatter targetFormatter;

		public ConvertingFormatter(Class<?> type, Class<?> formattedObjectType, Formatter targetFormatter) {
			this.type = type;
			this.formattedObjectType = formattedObjectType;
			this.targetFormatter = targetFormatter;
		}

		public String format(Object object, Locale locale) {
			object = conversionService.convert(object, formattedObjectType);
			return targetFormatter.format(object, locale);
		}

		public Object parse(String formatted, Locale locale) throws ParseException {
			Object parsed = targetFormatter.parse(formatted, locale);
			parsed = conversionService.convert(parsed, type);
			return parsed;
		}

	}

}