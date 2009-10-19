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
 * @see #setFormatters(Set)
 * @see #setFormatterMap(Map)
 * @see #setAnnotationFormatterMap(Map)
 * @see #setAnnotationFormatterFactories(Set)
 * @see #setConversionService(ConversionService)
 * @see #addFormatterByType(Formatter)
 * @see #addFormatterByType(Class, Formatter)
 * @see #addFormatterByAnnotation(Class, Formatter)
 * @see #addFormatterByAnnotation(AnnotationFormatterFactory) 
 */
public class GenericFormatterRegistry implements FormatterRegistry, ApplicationContextAware, Cloneable {

	private final Map<Class<?>, FormatterHolder> typeFormatters = new ConcurrentHashMap<Class<?>, FormatterHolder>();

	private final Map<Class<?>, AnnotationFormatterFactoryHolder> annotationFormatters = new ConcurrentHashMap<Class<?>, AnnotationFormatterFactoryHolder>();

	private ConversionService conversionService;

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
		if (this.conversionService == null
				&& context.containsBean(ConfigurableApplicationContext.CONVERSION_SERVICE_BEAN_NAME)) {
			this.conversionService = context.getBean(ConfigurableApplicationContext.CONVERSION_SERVICE_BEAN_NAME,
					ConversionService.class);
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

	public void addFormatterByType(Formatter<?> formatter) {
		Class<?> formatterObjectType = GenericTypeResolver.resolveTypeArgument(formatter.getClass(), Formatter.class);
		if (formatterObjectType == null) {
			throw new IllegalArgumentException("Unable to register Formatter " + formatter
					+ "; cannot determine parameterized object type <T>");
		}
		this.typeFormatters.put(formatterObjectType, new FormatterHolder(formatterObjectType, formatter));
	}

	public void addFormatterByType(Class<?> type, Formatter<?> formatter) {
		Class<?> formatterObjectType = GenericTypeResolver.resolveTypeArgument(formatter.getClass(), Formatter.class);
		if (formatterObjectType != null && !type.isAssignableFrom(formatterObjectType)) {
			if (this.conversionService == null) {
				throw new IllegalStateException("Unable to index Formatter " + formatter + " under type ["
						+ type.getName() + "]; not able to convert from a [" + formatterObjectType.getName()
						+ "] parsed by the Formatter to [" + type.getName()
						+ "] because this.conversionService is null");
			}
			if (!this.conversionService.canConvert(formatterObjectType, type)) {
				throw new IllegalArgumentException("Unable to index Formatter " + formatter + " under type ["
						+ type.getName() + "]; not able to convert from a [" + formatterObjectType.getName()
						+ "] parsed by the Formatter to [" + type.getName() + "]");
			}
			if (!this.conversionService.canConvert(type, formatterObjectType)) {
				throw new IllegalArgumentException("Unable to index Formatter " + formatter + " under type ["
						+ type.getName() + "]; not able to convert to [" + formatterObjectType.getName()
						+ "] to format a [" + type.getName() + "]");
			}
		}
		this.typeFormatters.put(type, new FormatterHolder(formatterObjectType, formatter));
	}

	public void addFormatterByAnnotation(Class<? extends Annotation> annotationType, Formatter<?> formatter) {
		Class<?> formatterObjectType = GenericTypeResolver.resolveTypeArgument(formatter.getClass(), Formatter.class);
		SimpleAnnotationFormatterFactory factory = new SimpleAnnotationFormatterFactory(formatter);
		this.annotationFormatters.put(annotationType,
				new AnnotationFormatterFactoryHolder(formatterObjectType, factory));
	}

	public void addFormatterByAnnotation(AnnotationFormatterFactory<?, ?> factory) {
		Class<?>[] typeArgs = GenericTypeResolver.resolveTypeArguments(factory.getClass(),
				AnnotationFormatterFactory.class);
		if (typeArgs == null || typeArgs.length != 2) {
			throw new IllegalArgumentException(
					"Unable to extract parameterized type arguments from AnnotationFormatterFactory ["
							+ factory.getClass().getName()
							+ "]; does the factory parameterize the <A> and <T> generic types?");
		}
		this.annotationFormatters.put(typeArgs[0], new AnnotationFormatterFactoryHolder(typeArgs[1], factory));
	}

	public Formatter<Object> getFormatter(TypeDescriptor type) {
		Assert.notNull(type, "TypeDescriptor is required");
		FormatterHolder holder = findFormatterHolderForAnnotatedProperty(type.getAnnotations());
		Class<?> objectType = type.getObjectType();
		if (holder == null) {
			holder = findFormatterHolderForType(objectType);
		}
		if (holder == null) {
			holder = getDefaultFormatterHolder(objectType);
		}
		if (holder == null) {
			return null;
		}
		Class<?> formatterObjectType = holder.getFormatterObjectType();
		if (formatterObjectType != null && !objectType.isAssignableFrom(formatterObjectType)) {
			if (this.conversionService != null) {
				return new ConvertingFormatter(type, holder);
			} else {
				return null;
			}
		} else {
			return holder.getFormatter();
		}
	}

	// internal helpers

	private FormatterHolder findFormatterHolderForAnnotatedProperty(Annotation[] annotations) {
		for (Annotation annotation : annotations) {
			FormatterHolder holder = findFormatterHolderForAnnotation(annotation);
			if (holder != null) {
				return holder;
			}
		}
		return null;
	}

	private FormatterHolder findFormatterHolderForAnnotation(Annotation annotation) {
		Class<? extends Annotation> annotationType = annotation.annotationType();
		AnnotationFormatterFactoryHolder factory = this.annotationFormatters.get(annotationType);
		if (factory != null) {
			return factory.getFormatterHolder(annotation);
		} else {
			Formatted formatted = annotationType.getAnnotation(Formatted.class);
			if (formatted != null) {
				// property annotation has @Formatted meta-annotation
				Formatter<?> formatter = createFormatter(formatted.value());
				addFormatterByAnnotation(annotationType, formatter);
				return findFormatterHolderForAnnotation(annotation);
			} else {
				return null;
			}
		}
	}

	private FormatterHolder findFormatterHolderForType(Class<?> type) {
		LinkedList<Class<?>> classQueue = new LinkedList<Class<?>>();
		classQueue.addFirst(type);
		while (!classQueue.isEmpty()) {
			Class<?> currentClass = classQueue.removeLast();
			FormatterHolder holder = this.typeFormatters.get(currentClass);
			if (holder != null) {
				return holder;
			}
			if (currentClass.getSuperclass() != null) {
				classQueue.addFirst(currentClass.getSuperclass());
			}
			Class<?>[] interfaces = currentClass.getInterfaces();
			for (Class<?> ifc : interfaces) {
				classQueue.addFirst(ifc);
			}
		}
		return null;
	}

	private FormatterHolder getDefaultFormatterHolder(Class<?> type) {
		Formatted formatted = AnnotationUtils.findAnnotation(type, Formatted.class);
		if (formatted != null) {
			Formatter<?> formatter = createFormatter(formatted.value());
			addFormatterByType(type, formatter);
			return findFormatterHolderForType(type);
		} else {
			return null;
		}
	}

	private Formatter<?> createFormatter(Class<? extends Formatter> formatterClass) {
		return (this.applicationContext != null ? this.applicationContext.getAutowireCapableBeanFactory().createBean(
				formatterClass) : BeanUtils.instantiate(formatterClass));
	}

	private abstract static class AbstractFormatterHolder {

		private Class<?> formatterObjectType;

		public AbstractFormatterHolder(Class<?> formatterObjectType) {
			this.formatterObjectType = formatterObjectType;
		}

		public Class<?> getFormatterObjectType() {
			return formatterObjectType;
		}

	}

	private static class FormatterHolder extends AbstractFormatterHolder {

		private Formatter formatter;

		public FormatterHolder(Class<?> formatterObjectType, Formatter<?> formatter) {
			super(formatterObjectType);
			this.formatter = formatter;
		}

		public Formatter getFormatter() {
			return this.formatter;
		}

	}

	private static class AnnotationFormatterFactoryHolder extends AbstractFormatterHolder {

		private AnnotationFormatterFactory factory;

		public AnnotationFormatterFactoryHolder(Class<?> formatterObjectType, AnnotationFormatterFactory<?, ?> factory) {
			super(formatterObjectType);
			this.factory = factory;
		}

		public FormatterHolder getFormatterHolder(Annotation annotation) {
			return new FormatterHolder(getFormatterObjectType(), this.factory.getFormatter(annotation));
		}

	}

	private static class SimpleAnnotationFormatterFactory implements AnnotationFormatterFactory {

		private final Formatter instance;

		public SimpleAnnotationFormatterFactory(Formatter<?> instance) {
			this.instance = instance;
		}

		public Formatter getFormatter(Annotation annotation) {
			return this.instance;
		}
	}

	private class ConvertingFormatter implements Formatter {

		private final TypeDescriptor type;

		private final FormatterHolder formatterHolder;

		public ConvertingFormatter(TypeDescriptor type, FormatterHolder formatterHolder) {
			this.type = type;
			this.formatterHolder = formatterHolder;
		}

		public String format(Object object, Locale locale) {
			object = GenericFormatterRegistry.this.conversionService.convert(object, this.formatterHolder
					.getFormatterObjectType());
			return this.formatterHolder.getFormatter().format(object, locale);
		}

		public Object parse(String formatted, Locale locale) throws ParseException {
			Object parsed = this.formatterHolder.getFormatter().parse(formatted, locale);
			parsed = GenericFormatterRegistry.this.conversionService.convert(parsed, TypeDescriptor
					.valueOf(this.formatterHolder.getFormatterObjectType()), this.type);
			return parsed;
		}
	}

}
