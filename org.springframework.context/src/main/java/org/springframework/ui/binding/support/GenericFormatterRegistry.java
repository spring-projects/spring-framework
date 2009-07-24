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

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.text.ParseException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.TypeConverter;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.ConversionUtils;
import org.springframework.core.convert.support.DefaultTypeConverter;
import org.springframework.ui.format.AnnotationFormatterFactory;
import org.springframework.ui.format.Formatted;
import org.springframework.ui.format.Formatter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

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

	private Map<CollectionTypeDescriptor, Formatter> collectionTypeFormatters = new ConcurrentHashMap<CollectionTypeDescriptor, Formatter>();

	private Map<Class, AnnotationFormatterFactory> annotationFormatters = new HashMap<Class, AnnotationFormatterFactory>();

	private TypeConverter typeConverter = new DefaultTypeConverter();

	public void setTypeConverter(TypeConverter typeConverter) {
		this.typeConverter = typeConverter;
	}

	public Formatter<?> getFormatter(PropertyDescriptor property) {
		Assert.notNull(property, "The PropertyDescriptor is required");
		TypeDescriptor<?> propertyType = new TypeDescriptor(new MethodParameter(property.getReadMethod(), -1));
		Annotation[] annotations = propertyType.getAnnotations();
		for (Annotation a : annotations) {
			AnnotationFormatterFactory factory = annotationFormatters.get(a.annotationType());
			if (factory != null) {
				return factory.getFormatter(a);
			}
		}
		Formatter<?> formatter = null;
		Class<?> type;
		if (propertyType.isCollection() || propertyType.isArray()) {
			CollectionTypeDescriptor collectionType = new CollectionTypeDescriptor(propertyType.getType(), propertyType
					.getElementType());
			formatter = collectionTypeFormatters.get(collectionType);
			if (formatter != null) {
				return formatter;
			} else {
				return new DefaultCollectionFormatter(collectionType, this);
			}
		} else {
			type = propertyType.getType();
		}
		return getFormatter(type);
	}

	public Formatter<?> getFormatter(Class<?> type) {
		Assert.notNull(type, "The Class of the object to format is required");
		Formatter formatter = typeFormatters.get(type);
		if (formatter != null) {
			return formatter;
		} else {
			Formatted formatted = AnnotationUtils.findAnnotation(type, Formatted.class);
			if (formatted != null) {
				Class formatterClass = formatted.value();
				try {
					formatter = (Formatter) formatterClass.newInstance();
				} catch (InstantiationException e) {
					throw new IllegalStateException(
							"Formatter referenced by @Formatted annotation does not have default constructor", e);
				} catch (IllegalAccessException e) {
					throw new IllegalStateException(
							"Formatter referenced by @Formatted annotation does not have public constructor", e);
				}
				typeFormatters.put(type, formatter);
				return formatter;
			} else {
				return new DefaultFormatter(type, typeConverter);
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

	public void add(CollectionTypeDescriptor propertyType, Formatter<?> formatter) {
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

	private static class DefaultFormatter implements Formatter {

		public static final Formatter DEFAULT_INSTANCE = new DefaultFormatter(null, null);

		private Class<?> objectType;

		private TypeConverter typeConverter;

		public DefaultFormatter(Class<?> objectType, TypeConverter typeConverter) {
			this.objectType = objectType;
			this.typeConverter = typeConverter;
		}

		public String format(Object object, Locale locale) {
			if (object == null) {
				return "";
			} else {
				if (typeConverter != null && typeConverter.canConvert(object.getClass(), String.class)) {
					return typeConverter.convert(object, String.class);
				} else {
					return object.toString();
				}
			}
		}

		public Object parse(String formatted, Locale locale) throws ParseException {
			if (formatted == "") {
				return null;
			} else {
				if (typeConverter != null && typeConverter.canConvert(String.class, objectType)) {
					try {
						return typeConverter.convert(formatted, objectType);
					} catch (ConversionFailedException e) {
						throw new ParseException(formatted, -1);
					}
				} else {
					return formatted;
				}
			}
		}
	}

	private static class DefaultCollectionFormatter implements Formatter {

		private CollectionTypeDescriptor collectionType;

		private Formatter elementFormatter;

		public DefaultCollectionFormatter(CollectionTypeDescriptor collectionType,
				GenericFormatterRegistry formatterRegistry) {
			this.collectionType = collectionType;
			this.elementFormatter = collectionType.getElementType() != null ? formatterRegistry
					.getFormatter(collectionType.getElementType()) : DefaultFormatter.DEFAULT_INSTANCE;
		}

		public String format(Object object, Locale locale) {
			if (object == null) {
				return "";
			} else {
				StringBuffer buffer = new StringBuffer();
				if (object.getClass().isArray()) {
					int length = Array.getLength(object);
					for (int i = 0; i < length; i++) {
						buffer.append(elementFormatter.format(Array.get(object, i), locale));
						if (i < length - 1) {
							buffer.append(",");
						}
					}
				} else if (Collection.class.isAssignableFrom(object.getClass())) {
					Collection c = (Collection) object;
					for (Iterator it = c.iterator(); it.hasNext();) {
						buffer.append(elementFormatter.format(it.next(), locale));
						if (it.hasNext()) {
							buffer.append(",");
						}
					}
				}
				return buffer.toString();
			}
		}

		public Object parse(String formatted, Locale locale) throws ParseException {
			String[] fields = StringUtils.commaDelimitedListToStringArray(formatted);
			if (collectionType.getType().isArray()) {
				Object array = Array.newInstance(getElementType(), fields.length);
				for (int i = 0; i < fields.length; i++) {
					Array.set(array, i, elementFormatter.parse(fields[i], locale));
				}
				return array;
			} else {
				Collection collection = newCollection();
				for (int i = 0; i < fields.length; i++) {
					collection.add(elementFormatter.parse(fields[i], locale));
				}
				return collection;
			}
		}

		private Class<?> getElementType() {
			if (collectionType.getElementType() != null) {
				return collectionType.getElementType();
			} else {
				return String.class;
			}
		}

		private Collection newCollection() {
			try {
				Class<? extends Collection> implType = ConversionUtils
						.getCollectionImpl((Class<? extends Collection>) collectionType.getType());
				return (Collection) implType.newInstance();
			} catch (InstantiationException e) {
				throw new IllegalStateException("Should not happen", e);
			} catch (IllegalAccessException e) {
				throw new IllegalStateException("Should not happen", e);
			}

		}
	};

}