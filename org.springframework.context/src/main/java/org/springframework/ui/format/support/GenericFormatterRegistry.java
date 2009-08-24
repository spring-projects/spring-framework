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

package org.springframework.ui.format.support;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.util.Assert;
import org.springframework.ui.format.FormatterRegistry;
import org.springframework.ui.format.Formatter;
import org.springframework.ui.format.AnnotationFormatterFactory;
import org.springframework.ui.format.Formatted;

/**
 * A generic implementation of {@link org.springframework.ui.format.FormatterRegistry} suitable for use in most environments.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 * @see #setConversionService(ConversionService)
 * @see #add(org.springframework.ui.format.Formatter)
 * @see #add(Class, org.springframework.ui.format.Formatter)
 * @see #add(org.springframework.ui.format.AnnotationFormatterFactory)
 */
public class GenericFormatterRegistry implements FormatterRegistry, BeanFactoryAware, Cloneable {

	private final Map<Class, Formatter> typeFormatters = new ConcurrentHashMap<Class, Formatter>();

	private final Map<Class, AnnotationFormatterFactory> annotationFormatters =
			new ConcurrentHashMap<Class, AnnotationFormatterFactory>();

	private ConversionService conversionService = new DefaultConversionService();

	private boolean shared = true;


	/**
	 * Registers the formatters in the set provided.
	 * JavaBean-friendly alternative to calling {@link #add(Formatter)}.
	 * @see #add(Formatter)
	 */
	public void setFormatters(Set<Formatter<?>> formatters) {
		for (Formatter<?> formatter : formatters) {
			add(formatter);
		}
	}

	/**
	 * Registers the formatters in the map provided by type.
	 * JavaBean-friendly alternative to calling {@link #add(Class, Formatter)}.
	 * @see #add(Class, Formatter)
	 */
	public void setFormatterMap(Map<Class<?>, Formatter<?>> formatters) {
		for (Map.Entry<Class<?>, Formatter<?>> entry : formatters.entrySet()) {
			add(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * Registers the annotation formatter factories in the set provided.
	 * JavaBean-friendly alternative to calling {@link #add(AnnotationFormatterFactory)}.
	 * @see #add(AnnotationFormatterFactory)
	 */
	public void setAnnotationFormatterFactories(Set<AnnotationFormatterFactory> factories) {
		for (AnnotationFormatterFactory factory : factories) {
			add(factory);
		}
	}

	/**
	 * Specify the type conversion service that will be used to coerce objects to the
	 * types required for formatting. Defaults to a {@link DefaultConversionService}.
	 * @see #add(Class, Formatter)
	 */
	public void setConversionService(ConversionService conversionService) {
		Assert.notNull(conversionService, "ConversionService must not be null");
		this.conversionService = conversionService;
	}

	/**
	 * Return the type conversion service which this FormatterRegistry delegates to.
	 */
	public ConversionService getConversionService() {
		return this.conversionService;
	}

	/**
	 * Take the context's default ConversionService if none specified locally.
	 */
	public void setBeanFactory(BeanFactory beanFactory) {
		if (this.conversionService == null &&
				beanFactory.containsBean(ConfigurableApplicationContext.CONVERSION_SERVICE_BEAN_NAME)) {
			this.conversionService = beanFactory.getBean(
					ConfigurableApplicationContext.CONVERSION_SERVICE_BEAN_NAME, ConversionService.class);
		}
	}


	// cloning support

	/**
	 * Specify whether this FormatterRegistry is shared, in which case newly
	 * registered Formatters will be visible to other callers as well.
	 * <p>A new GenericFormatterRegistry is considered as shared by default,
	 * whereas a cloned GenericFormatterRegistry will be non-shared by default.
	 * @see #clone()
	 */
	public void setShared(boolean shared) {
		this.shared = shared;
	}

	/**
	 * Return whether this FormatterRegistry is shared, in which case newly
	 * registered Formatters will be visible to other callers as well.
	 */
	public boolean isShared() {
		return this.shared;
	}

	/**
	 * Create an independent clone of this FormatterRegistry.
	 * @see #setShared
	 */
	@Override
	public GenericFormatterRegistry clone() {
		GenericFormatterRegistry clone = new GenericFormatterRegistry();
		clone.typeFormatters.putAll(this.typeFormatters);
		clone.annotationFormatters.putAll(this.annotationFormatters);
		clone.conversionService = this.conversionService;
		clone.shared = false;
		return clone;
	}


	// implementing FormatterRegistry

	public <T> void add(Formatter<T> formatter) {
		this.typeFormatters.put(getFormattedObjectType(formatter.getClass()), formatter);
	}

	public void add(Class<?> type, Formatter<?> formatter) {
		Class<?> formattedObjectType = getFormattedObjectType(formatter.getClass());
		if (!this.conversionService.canConvert(formattedObjectType, type)) {
			throw new IllegalArgumentException("Unable to register formatter " + formatter + " for type [" + type.getName() + "]; not able to convert from [" + formattedObjectType.getName() + "] to parse");
		}
		if (!this.conversionService.canConvert(type, formattedObjectType)) {
			throw new IllegalArgumentException("Unable to register formatter " + formatter + " for type [" + type.getName() + "]; not able to convert to [" + formattedObjectType.getName() + "] to format");
		}		
		this.typeFormatters.put(type, formatter);
	}

	public void add(AnnotationFormatterFactory<?, ?> factory) {
		this.annotationFormatters.put(getAnnotationType(factory.getClass()), factory);
	}

	@SuppressWarnings("unchecked")
	public Formatter<Object> getFormatter(TypeDescriptor type) {
		Assert.notNull(type, "The TypeDescriptor is required");
		Formatter<Object> formatter = getAnnotationFormatter(type);
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

	@SuppressWarnings("unchecked")
	private Formatter getAnnotationFormatter(TypeDescriptor type) {
		Annotation[] annotations = type.getAnnotations();
		for (Annotation a : annotations) {
			AnnotationFormatterFactory factory = annotationFormatters.get(a.annotationType());
			if (factory != null) {
				return factory.getFormatter(a);
			}
		}
		return null;
	}
	
	private Formatter getTypeFormatter(Class<?> type) {
		Assert.notNull(type, "The Class of the object to format is required");
		Formatter formatter = findFormatter(type);
		if (formatter != null) {
			Class<?> formattedObjectType = getFormattedObjectType(formatter.getClass());
			if (type.isAssignableFrom(formattedObjectType)) {
				return formatter;
			}
			else {
				return new ConvertingFormatter(type, formattedObjectType, formatter);
			}
		}
		else {
			return getDefaultFormatter(type);
		}
	}
	
	private Formatter<?> findFormatter(Class<?> type) {
		LinkedList<Class> classQueue = new LinkedList<Class>();
		classQueue.addFirst(type);
		while (!classQueue.isEmpty()) {
			Class currentClass = classQueue.removeLast();
			Formatter<?> formatter = typeFormatters.get(currentClass);
			if (formatter != null) {
				return formatter;
			}
			if (currentClass.getSuperclass() != null) {
				classQueue.addFirst(currentClass.getSuperclass());
			}
			Class[] interfaces = currentClass.getInterfaces();
			for (Class ifc : interfaces) {
				classQueue.addFirst(ifc);
			}
		}
		return null;
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
		}
		else {
			return null;
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

		@SuppressWarnings("unchecked")
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
