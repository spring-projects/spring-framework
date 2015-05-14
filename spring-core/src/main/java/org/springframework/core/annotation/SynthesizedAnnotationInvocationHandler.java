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
import java.util.Map;

import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

/**
 * TODO Document SynthesizedAnnotationInvocationHandler.
 *
 * @author Sam Brannen
 * @since 4.2
 */
class SynthesizedAnnotationInvocationHandler implements InvocationHandler {

	private final AnnotatedElement annotatedElement;

	private final Annotation annotation;

	private final Map<String, String> aliasPairs;


	public SynthesizedAnnotationInvocationHandler(Annotation annotation, Map<String, String> aliasPairs) {
		this(null, annotation, aliasPairs);
	}

	public SynthesizedAnnotationInvocationHandler(AnnotatedElement annotatedElement, Annotation annotation,
			Map<String, String> aliasPairs) {
		this.annotatedElement = annotatedElement;
		this.annotation = annotation;
		this.aliasPairs = aliasPairs;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		String attributeName = method.getName();
		Class<?> returnType = method.getReturnType();
		boolean nestedAnnotation = (Annotation[].class.isAssignableFrom(returnType) || Annotation.class.isAssignableFrom(returnType));
		String aliasedAttributeName = aliasPairs.get(attributeName);
		boolean aliasPresent = aliasedAttributeName != null;

		ReflectionUtils.makeAccessible(method);
		Object value = ReflectionUtils.invokeMethod(method, this.annotation, args);

		// Nothing special to do?
		if (!aliasPresent && !nestedAnnotation) {
			return value;
		}

		if (aliasPresent) {
			Method aliasedMethod = null;
			try {
				aliasedMethod = annotation.annotationType().getDeclaredMethod(aliasedAttributeName);
			}
			catch (NoSuchMethodException e) {
				String msg = String.format("In annotation [%s], attribute [%s] is declared as an @AliasFor [%s], "
						+ "but attribute [%s] does not exist.", annotation.annotationType().getName(), attributeName,
					aliasedAttributeName, aliasedAttributeName);
				throw new AnnotationConfigurationException(msg);
			}

			ReflectionUtils.makeAccessible(aliasedMethod);
			Object aliasedValue = ReflectionUtils.invokeMethod(aliasedMethod, this.annotation, args);
			Object defaultValue = AnnotationUtils.getDefaultValue(annotation, attributeName);

			if (!ObjectUtils.nullSafeEquals(value, aliasedValue) && !ObjectUtils.nullSafeEquals(value, defaultValue)
					&& !ObjectUtils.nullSafeEquals(aliasedValue, defaultValue)) {
				String elementAsString = (annotatedElement == null ? "unknown element" : annotatedElement.toString());
				String msg = String.format(
					"In annotation [%s] declared on [%s], attribute [%s] and its alias [%s] are "
							+ "declared with values of [%s] and [%s], but only one declaration is permitted.",
					annotation.annotationType().getName(), elementAsString, attributeName, aliasedAttributeName,
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
			value = AnnotationUtils.synthesizeAnnotation(annotatedElement, (Annotation) value);
		}
		else if (value instanceof Annotation[]) {
			Annotation[] annotations = (Annotation[]) value;
			for (int i = 0; i < annotations.length; i++) {
				annotations[i] = AnnotationUtils.synthesizeAnnotation(annotatedElement, annotations[i]);
			}
		}

		return value;
	}

}
