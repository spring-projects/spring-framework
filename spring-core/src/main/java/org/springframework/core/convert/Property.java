/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.core.convert;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodParameter;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * A description of a JavaBeans Property that allows us to avoid a dependency on
 * {@code java.beans.PropertyDescriptor}. The {@code java.beans} package
 * is not available in a number of environments (for example, Android, Java ME), so this is
 * desirable for portability of Spring's core conversion facility.
 *
 * <p>Used to build a {@link TypeDescriptor} from a property location. The built
 * {@code TypeDescriptor} can then be used to convert from/to the property type.
 *
 * @author Keith Donald
 * @author Phillip Webb
 * @since 3.1
 * @see TypeDescriptor#TypeDescriptor(Property)
 * @see TypeDescriptor#nested(Property, int)
 */
public final class Property {

	private static final Map<Property, Annotation[]> annotationCache = new ConcurrentReferenceHashMap<>();

	private final Class<?> objectType;

	private final @Nullable Method readMethod;

	private final @Nullable Method writeMethod;

	private final String name;

	private final MethodParameter methodParameter;

	private Annotation @Nullable [] annotations;


	public Property(Class<?> objectType, @Nullable Method readMethod, @Nullable Method writeMethod) {
		this(objectType, readMethod, writeMethod, null);
	}

	public Property(
			Class<?> objectType, @Nullable Method readMethod, @Nullable Method writeMethod, @Nullable String name) {

		this.objectType = objectType;
		this.readMethod = readMethod;
		this.writeMethod = writeMethod;
		this.methodParameter = resolveMethodParameter();
		this.name = (name != null ? name : resolveName());
	}


	/**
	 * The object declaring this property, either directly or in a superclass the object extends.
	 */
	public Class<?> getObjectType() {
		return this.objectType;
	}

	/**
	 * The name of the property: for example, 'foo'.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * The property type: for example, {@code java.lang.String}.
	 */
	public Class<?> getType() {
		return this.methodParameter.getParameterType();
	}

	/**
	 * The property getter method: for example, {@code getFoo()}.
	 */
	public @Nullable Method getReadMethod() {
		return this.readMethod;
	}

	/**
	 * The property setter method: for example, {@code setFoo(String)}.
	 */
	public @Nullable Method getWriteMethod() {
		return this.writeMethod;
	}


	// Package private

	MethodParameter getMethodParameter() {
		return this.methodParameter;
	}

	Annotation[] getAnnotations() {
		if (this.annotations == null) {
			this.annotations = resolveAnnotations();
		}
		return this.annotations;
	}


	// Internal helpers

	private String resolveName() {
		if (this.readMethod != null) {
			int index = this.readMethod.getName().indexOf("get");
			if (index != -1) {
				index += 3;
			}
			else {
				index = this.readMethod.getName().indexOf("is");
				if (index != -1) {
					index += 2;
				}
				else {
					// Record-style plain accessor method, for example, name()
					index = 0;
				}
			}
			return StringUtils.uncapitalize(this.readMethod.getName().substring(index));
		}
		else if (this.writeMethod != null) {
			int index = this.writeMethod.getName().indexOf("set");
			if (index == -1) {
				throw new IllegalArgumentException("Not a setter method");
			}
			index += 3;
			return StringUtils.uncapitalize(this.writeMethod.getName().substring(index));
		}
		else {
			throw new IllegalStateException("Property is neither readable nor writeable");
		}
	}

	private MethodParameter resolveMethodParameter() {
		MethodParameter read = resolveReadMethodParameter();
		MethodParameter write = resolveWriteMethodParameter();
		if (write == null) {
			if (read == null) {
				throw new IllegalStateException("Property is neither readable nor writeable");
			}
			return read;
		}
		if (read != null) {
			Class<?> readType = read.getParameterType();
			Class<?> writeType = write.getParameterType();
			if (!writeType.equals(readType) && writeType.isAssignableFrom(readType)) {
				return read;
			}
		}
		return write;
	}

	private @Nullable MethodParameter resolveReadMethodParameter() {
		if (getReadMethod() == null) {
			return null;
		}
		return new MethodParameter(getReadMethod(), -1).withContainingClass(getObjectType());
	}

	private @Nullable MethodParameter resolveWriteMethodParameter() {
		if (getWriteMethod() == null) {
			return null;
		}
		return new MethodParameter(getWriteMethod(), 0).withContainingClass(getObjectType());
	}

	private Annotation[] resolveAnnotations() {
		Annotation[] annotations = annotationCache.get(this);
		if (annotations == null) {
			Map<Class<? extends Annotation>, Annotation> annotationMap = new LinkedHashMap<>();
			addAnnotationsToMap(annotationMap, getReadMethod());
			addAnnotationsToMap(annotationMap, getWriteMethod());
			addAnnotationsToMap(annotationMap, getField());
			annotations = annotationMap.values().toArray(new Annotation[0]);
			annotationCache.put(this, annotations);
		}
		return annotations;
	}

	private void addAnnotationsToMap(
			Map<Class<? extends Annotation>, Annotation> annotationMap, @Nullable AnnotatedElement object) {

		if (object != null) {
			for (Annotation annotation : object.getAnnotations()) {
				annotationMap.put(annotation.annotationType(), annotation);
			}
		}
	}

	private @Nullable Field getField() {
		String name = getName();
		if (!StringUtils.hasLength(name)) {
			return null;
		}
		Field field = null;
		Class<?> declaringClass = declaringClass();
		if (declaringClass != null) {
			field = ReflectionUtils.findField(declaringClass, name);
			if (field == null) {
				// Same lenient fallback checking as in CachedIntrospectionResults...
				field = ReflectionUtils.findField(declaringClass, StringUtils.uncapitalize(name));
				if (field == null) {
					field = ReflectionUtils.findField(declaringClass, StringUtils.capitalize(name));
				}
			}
		}
		return field;
	}

	private @Nullable Class<?> declaringClass() {
		if (getReadMethod() != null) {
			return getReadMethod().getDeclaringClass();
		}
		else if (getWriteMethod() != null) {
			return getWriteMethod().getDeclaringClass();
		}
		else {
			return null;
		}
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof Property that &&
				ObjectUtils.nullSafeEquals(this.objectType, that.objectType) &&
				ObjectUtils.nullSafeEquals(this.name, that.name) &&
				ObjectUtils.nullSafeEquals(this.readMethod, that.readMethod) &&
				ObjectUtils.nullSafeEquals(this.writeMethod, that.writeMethod)));
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.objectType, this.name);
	}

}
