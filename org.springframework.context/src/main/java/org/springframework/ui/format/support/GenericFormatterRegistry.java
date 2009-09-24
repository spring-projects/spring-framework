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
import java.text.ParseException;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.ui.format.AnnotationFormatterFactory;
import org.springframework.ui.format.Formatted;
import org.springframework.ui.format.Formatter;
import org.springframework.ui.format.FormatterRegistry;
import org.springframework.util.Assert;

/**
 * A generic implementation of {@link org.springframework.ui.format.FormatterRegistry}
 * suitable for use in most environments.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 * @see #setConversionService(ConversionService)
 * @see #add(org.springframework.ui.format.Formatter)
 * @see #add(Class, org.springframework.ui.format.Formatter)
 * @see #add(org.springframework.ui.format.AnnotationFormatterFactory)
 */
public class GenericFormatterRegistry implements FormatterRegistry, ApplicationContextAware, Cloneable {

	private final Map<Class, Formatter> typeFormatters = new ConcurrentHashMap<Class, Formatter>();

	private final Map<Class, AnnotationFormatterFactory> annotationFormatters =
			new ConcurrentHashMap<Class, AnnotationFormatterFactory>();

	private ConversionService conversionService = new DefaultConversionService();

	private ApplicationContext applicationContext;

	private boolean shared = true;


	/**
	 * Registers the formatters in the set provided.
	 * JavaBean-friendly alternative to calling {@link #addFormatterByType(Formatter)}.
	 * @see #add(Formatter)
	 */
	public void setFormatters(Set<Formatter<?>> formatters) {
		for (Formatter<?> formatter : formatters) {
			addFormatterByType(formatter);
		}
	}

	/**
	 * Registers the formatters in the map provided by type.
	 * JavaBean-friendly alternative to calling {@link #addFormatterByType(Class, Formatter)}.
	 * @see #add(Class, Formatter)
	 */
	public void setFormatterMap(Map<Class<?>, Formatter<?>> formatters) {
		for (Map.Entry<Class<?>, Formatter<?>> entry : formatters.entrySet()) {
			addFormatterByType(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * Registers the formatters in the map provided by annotation type.
	 * JavaBean-friendly alternative to calling {@link #addFormatterByAnnotation(Class, Formatter)}.
	 * @see #add(Class, Formatter)
	 */
	public void setAnnotationFormatterMap(Map<Class<? extends Annotation>, Formatter<?>> formatters) {
		for (Map.Entry<Class<? extends Annotation>, Formatter<?>> entry : formatters.entrySet()) {
			addFormatterByAnnotation(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * Registers the annotation formatter factories in the set provided.
	 * JavaBean-friendly alternative to calling {@link #addFormatterByAnnotation(AnnotationFormatterFactory)}.
	 * @see #add(AnnotationFormatterFactory)
	 */
	public void setAnnotationFormatterFactories(Set<AnnotationFormatterFactory<?, ?>> factories) {
		for (AnnotationFormatterFactory<?, ?> factory : factories) {
			addFormatterByAnnotation(factory);
		}
	}

	/**
	 * Specify the type conversion service that will be used to coerce objects to the
	 * types required for formatting. Defaults to a {@link DefaultConversionService}.
	 * @see #addFormatterByType(Class, Formatter)
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
	public void setApplicationContext(ApplicationContext context) {
		if (this.conversionService == null &&
				context.containsBean(ConfigurableApplicationContext.CONVERSION_SERVICE_BEAN_NAME)) {
			this.conversionService = context.getBean(
					ConfigurableApplicationContext.CONVERSION_SERVICE_BEAN_NAME, ConversionService.class);
		}
		this.applicationContext = context;
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
		clone.applicationContext = applicationContext;
		clone.shared = false;
		return clone;
	}


	// implementing FormatterRegistry

	public void addFormatterByType(Class<?> type, Formatter<?> formatter) {
		Class<?> formattedObjectType = GenericTypeResolver.resolveTypeArgument(formatter.getClass(), Formatter.class);
		if (!this.conversionService.canConvert(formattedObjectType, type)) {
			throw new IllegalArgumentException("Unable to register Formatter " + formatter + " for type [" +
					type.getName() + "]; not able to convert from [" + formattedObjectType.getName() + "] to parse");
		}
		if (!this.conversionService.canConvert(type, formattedObjectType)) {
			throw new IllegalArgumentException("Unable to register Formatter " + formatter + " for type [" +
					type.getName() + "]; not able to convert to [" + formattedObjectType.getName() + "] to format");
		}		
		this.typeFormatters.put(type, formatter);
	}

	public <T> void addFormatterByType(Formatter<T> formatter) {
		Class<?> formattedObjectType = GenericTypeResolver.resolveTypeArgument(formatter.getClass(), Formatter.class);
		this.typeFormatters.put(formattedObjectType, formatter);
	}

	public void addFormatterByAnnotation(Class<? extends Annotation> annotationType, Formatter<?> formatter) {
		this.annotationFormatters.put(annotationType, new SimpleAnnotationFormatterFactory(formatter));
	}

	public <A extends Annotation, T> void addFormatterByAnnotation(AnnotationFormatterFactory<A, T> factory) {
		Class[] typeArgs = GenericTypeResolver.resolveTypeArguments(factory.getClass(), AnnotationFormatterFactory.class);
		if (typeArgs == null) {
			throw new IllegalArgumentException(
					"Unable to extract Annotation type A argument from AnnotationFormatterFactory [" +
							factory.getClass().getName() + "]; does the factory parameterize the <A> generic type?");
		}
		this.annotationFormatters.put(typeArgs[0], factory);
	}

	@SuppressWarnings("unchecked")
	public <T> Formatter<T> getFormatter(Class<T> targetType) {
		return (Formatter<T>) getFormatter(TypeDescriptor.valueOf(targetType));
	}

	@SuppressWarnings("unchecked")
	public Formatter<Object> getFormatter(TypeDescriptor type) {
		Assert.notNull(type, "TypeDescriptor is required");
		Formatter<Object> formatter = getAnnotationFormatter(type);
		if (formatter == null) {
			formatter = getTypeFormatter(type.getType());
		}
		if (formatter != null) {
			Class<?> formattedObjectType = GenericTypeResolver.resolveTypeArgument(formatter.getClass(), Formatter.class);
			if (!type.getType().isAssignableFrom(formattedObjectType)) {
				return new ConvertingFormatter(type.getType(), formattedObjectType, formatter);
			}
		}
		return formatter;
	}


	// internal helpers

	@SuppressWarnings("unchecked")
	private Formatter getAnnotationFormatter(TypeDescriptor type) {
		Annotation[] annotations = type.getAnnotations();
		for (Annotation ann : annotations) {
			AnnotationFormatterFactory factory = this.annotationFormatters.get(ann.annotationType());
			if (factory != null) {
				return factory.getFormatter(ann);
			}
			else {
				Formatted formattedAnnotation = ann.annotationType().getAnnotation(Formatted.class);
				if (formattedAnnotation != null) {
					Formatter formatter = createFormatter(formattedAnnotation.value());
					this.annotationFormatters.put(ann.annotationType(), new SimpleAnnotationFormatterFactory(formatter));
					return formatter;
				}
			}
		}
		return null;
	}

	private Formatter getTypeFormatter(Class<?> type) {
		Formatter formatter = findFormatter(type);
		return (formatter != null ? formatter : getDefaultFormatter(type));
	}
	
	private Formatter<?> findFormatter(Class<?> type) {
		LinkedList<Class> classQueue = new LinkedList<Class>();
		classQueue.addFirst(type);
		while (!classQueue.isEmpty()) {
			Class currentClass = classQueue.removeLast();
			Formatter<?> formatter = this.typeFormatters.get(currentClass);
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
			Formatter formatter = createFormatter(formatted.value());
			this.typeFormatters.put(type, formatter);
			return formatter;
		}
		else {
			return null;
		}
	}

	private Formatter<?> createFormatter(Class<? extends Formatter> formatterClass) {
		return (this.applicationContext != null ?
				this.applicationContext.getAutowireCapableBeanFactory().createBean(formatterClass) :
				BeanUtils.instantiate(formatterClass));
	}


	private class ConvertingFormatter implements Formatter {

		private final Class<?> type;

		private final Class<?> formattedObjectType;

		private final Formatter targetFormatter;

		public ConvertingFormatter(Class<?> type, Class<?> formattedObjectType, Formatter targetFormatter) {
			this.type = type;
			this.formattedObjectType = formattedObjectType;
			this.targetFormatter = targetFormatter;
		}

		@SuppressWarnings("unchecked")
		public String format(Object object, Locale locale) {
			object = conversionService.convert(object, this.formattedObjectType);
			return this.targetFormatter.format(object, locale);
		}

		public Object parse(String formatted, Locale locale) throws ParseException {
			Object parsed = this.targetFormatter.parse(formatted, locale);
			parsed = conversionService.convert(parsed, this.type);
			return parsed;
		}
	}


	private static class SimpleAnnotationFormatterFactory implements AnnotationFormatterFactory {

		private final Formatter instance;

		public SimpleAnnotationFormatterFactory(Formatter instance) {
			this.instance = instance;
		}

		public Formatter getFormatter(Annotation annotation) {
			return this.instance;
		}
	}

}
