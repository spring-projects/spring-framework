/*
 * Copyright 2002-2015 the original author or authors.
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
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;

import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import static org.springframework.core.annotation.AnnotationUtils.*;
import static org.springframework.util.ReflectionUtils.*;

/**
 * {@link InvocationHandler} for an {@link Annotation} that Spring has
 * <em>synthesized</em> (i.e., wrapped in a dynamic proxy) with additional
 * functionality.
 *
 * <p>{@code SynthesizedAnnotationInvocationHandler} transparently enforces
 * attribute alias semantics for annotation attributes that are annotated
 * with {@link AliasFor @AliasFor}. In addition, nested annotations and
 * arrays of nested annotations will be synthesized upon first access (i.e.,
 * <em>lazily</em>).
 *
 * @author Sam Brannen
 * @since 4.2
 * @see Annotation
 * @see AliasFor
 * @see AnnotationUtils#synthesizeAnnotation(Annotation, AnnotatedElement)
 */
class SynthesizedAnnotationInvocationHandler implements InvocationHandler {

	private final AnnotatedElement annotatedElement;

	private final Annotation annotation;

	private final Class<? extends Annotation> annotationType;

	private final Map<String, String> aliasMap;


	SynthesizedAnnotationInvocationHandler(AnnotatedElement annotatedElement, Annotation annotation,
			Map<String, String> aliasMap) {
		this.annotatedElement = annotatedElement;
		this.annotation = annotation;
		this.annotationType = annotation.annotationType();
		this.aliasMap = aliasMap;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if (isEqualsMethod(method)) {
			return equals(proxy, args[0]);
		}
		if (isToStringMethod(method)) {
			return toString(proxy);
		}

		String methodName = method.getName();
		Class<?> returnType = method.getReturnType();
		boolean nestedAnnotation = (Annotation[].class.isAssignableFrom(returnType) || Annotation.class.isAssignableFrom(returnType));
		String aliasedAttributeName = aliasMap.get(methodName);
		boolean aliasPresent = (aliasedAttributeName != null);

		makeAccessible(method);
		Object value = invokeMethod(method, this.annotation, args);

		// No custom processing necessary?
		if (!aliasPresent && !nestedAnnotation) {
			return value;
		}

		if (aliasPresent) {
			Method aliasedMethod = null;
			try {
				aliasedMethod = this.annotationType.getDeclaredMethod(aliasedAttributeName);
			}
			catch (NoSuchMethodException e) {
				String msg = String.format("In annotation [%s], attribute [%s] is declared as an @AliasFor [%s], "
						+ "but attribute [%s] does not exist.", this.annotationType.getName(), methodName,
					aliasedAttributeName, aliasedAttributeName);
				throw new AnnotationConfigurationException(msg);
			}

			makeAccessible(aliasedMethod);
			Object aliasedValue = invokeMethod(aliasedMethod, this.annotation, args);
			Object defaultValue = getDefaultValue(this.annotation, methodName);

			if (!ObjectUtils.nullSafeEquals(value, aliasedValue) && !ObjectUtils.nullSafeEquals(value, defaultValue)
					&& !ObjectUtils.nullSafeEquals(aliasedValue, defaultValue)) {
				String elementName = (this.annotatedElement == null ? "unknown element"
						: this.annotatedElement.toString());
				String msg = String.format(
					"In annotation [%s] declared on [%s], attribute [%s] and its alias [%s] are "
							+ "declared with values of [%s] and [%s], but only one declaration is permitted.",
					this.annotationType.getName(), elementName, methodName, aliasedAttributeName,
					ObjectUtils.nullSafeToString(value), ObjectUtils.nullSafeToString(aliasedValue));
				throw new AnnotationConfigurationException(msg);
			}

			// If the user didn't declare the annotation with an explicit value, return
			// the value of the alias.
			if (ObjectUtils.nullSafeEquals(value, defaultValue)) {
				value = aliasedValue;
			}
		}

		// Synthesize nested annotations before returning them.
		if (value instanceof Annotation) {
			value = synthesizeAnnotation((Annotation) value, this.annotatedElement);
		}
		else if (value instanceof Annotation[]) {
			Annotation[] annotations = (Annotation[]) value;
			for (int i = 0; i < annotations.length; i++) {
				annotations[i] = synthesizeAnnotation(annotations[i], this.annotatedElement);
			}
		}

		return value;
	}

	private boolean equals(Object proxy, Object other) {
		if (this == other) {
			return true;
		}
		if (!this.annotationType.isInstance(other)) {
			return false;
		}

		for (Method attributeMethod : getAttributeMethods(this.annotationType)) {
			Object thisValue = invokeMethod(attributeMethod, proxy);
			Object otherValue = invokeMethod(attributeMethod, other);
			if (!ObjectUtils.nullSafeEquals(thisValue, otherValue)) {
				return false;
			}
		}

		return true;
	}

	private String toString(Object proxy) {
		StringBuilder sb = new StringBuilder("@").append(annotationType.getName()).append("(");

		Iterator<Method> iterator = getAttributeMethods(this.annotationType).iterator();
		while (iterator.hasNext()) {
			Method attributeMethod = iterator.next();
			sb.append(attributeMethod.getName());
			sb.append('=');
			sb.append(valueToString(invokeMethod(attributeMethod, proxy)));
			sb.append(iterator.hasNext() ? ", " : "");
		}

		return sb.append(")").toString();
	}

	private String valueToString(Object value) {
		if (value instanceof Object[]) {
			return "[" + StringUtils.arrayToDelimitedString((Object[]) value, ", ") + "]";
		}

		// else
		return String.valueOf(value);
	}

}
