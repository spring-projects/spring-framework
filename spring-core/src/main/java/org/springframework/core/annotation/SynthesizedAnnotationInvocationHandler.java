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
import java.util.Arrays;
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
		if (isHashCodeMethod(method)) {
			return hashCode(proxy);
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
			Object aliasedValue = invokeMethod(aliasedMethod, this.annotation);
			Object defaultValue = getDefaultValue(this.annotation, methodName);

			if (!nullSafeEquals(value, aliasedValue) && !nullSafeEquals(value, defaultValue)
					&& !nullSafeEquals(aliasedValue, defaultValue)) {
				String elementName = (this.annotatedElement == null ? "unknown element"
						: this.annotatedElement.toString());
				String msg = String.format(
					"In annotation [%s] declared on [%s], attribute [%s] and its alias [%s] are "
							+ "declared with values of [%s] and [%s], but only one declaration is permitted.",
					this.annotationType.getName(), elementName, methodName, aliasedAttributeName,
					nullSafeToString(value), nullSafeToString(aliasedValue));
				throw new AnnotationConfigurationException(msg);
			}

			// If the user didn't declare the annotation with an explicit value, return
			// the value of the alias.
			if (nullSafeEquals(value, defaultValue)) {
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

	/**
	 * See {@link Annotation#equals(Object)} for a definition of the required algorithm.
	 *
	 * @param proxy the synthesized annotation
	 * @param other the other object to compare against
	 */
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
			if (!nullSafeEquals(thisValue, otherValue)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * See {@link Annotation#hashCode()} for a definition of the required algorithm.
	 *
	 * @param proxy the synthesized annotation
	 */
	private int hashCode(Object proxy) {
		int result = 0;

		for (Method attributeMethod : getAttributeMethods(this.annotationType)) {
			Object value = invokeMethod(attributeMethod, proxy);
			int hashCode;
			if (value.getClass().isArray()) {
				hashCode = hashCodeForArray(value);
			}
			else {
				hashCode = value.hashCode();
			}
			result += (127 * attributeMethod.getName().hashCode()) ^ hashCode;
		}

		return result;
	}

	/**
	 * WARNING: we can NOT use any of the {@code nullSafeHashCode()} methods
	 * in Spring's {@link ObjectUtils} because those hash code generation
	 * algorithms do not comply with the requirements specified in
	 * {@link Annotation#hashCode()}.
	 *
	 * @param array the array to compute the hash code for
	 */
	private int hashCodeForArray(Object array) {
		if (array instanceof boolean[]) {
			return Arrays.hashCode((boolean[]) array);
		}
		if (array instanceof byte[]) {
			return Arrays.hashCode((byte[]) array);
		}
		if (array instanceof char[]) {
			return Arrays.hashCode((char[]) array);
		}
		if (array instanceof double[]) {
			return Arrays.hashCode((double[]) array);
		}
		if (array instanceof float[]) {
			return Arrays.hashCode((float[]) array);
		}
		if (array instanceof int[]) {
			return Arrays.hashCode((int[]) array);
		}
		if (array instanceof long[]) {
			return Arrays.hashCode((long[]) array);
		}
		if (array instanceof short[]) {
			return Arrays.hashCode((short[]) array);
		}

		// else
		return Arrays.hashCode((Object[]) array);
	}

	/**
	 * See {@link Annotation#toString()} for guidelines on the recommended format.
	 *
	 * @param proxy the synthesized annotation
	 */
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

	private static boolean nullSafeEquals(Object o1, Object o2) {
		return ObjectUtils.nullSafeEquals(o1, o2);
	}

	private static String nullSafeToString(Object obj) {
		return ObjectUtils.nullSafeToString(obj);
	}

}
