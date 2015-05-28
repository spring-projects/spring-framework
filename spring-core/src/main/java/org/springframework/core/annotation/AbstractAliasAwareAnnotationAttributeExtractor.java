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
import java.lang.reflect.Method;
import java.util.Map;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Abstract base class for {@link AnnotationAttributeExtractor} implementations
 * that transparently enforce attribute alias semantics for annotation
 * attributes that are annotated with {@link AliasFor @AliasFor}.
 *
 * @author Sam Brannen
 * @since 4.2
 * @see Annotation
 * @see AliasFor
 * @see AnnotationUtils#synthesizeAnnotation(Annotation, AnnotatedElement)
 */
abstract class AbstractAliasAwareAnnotationAttributeExtractor implements AnnotationAttributeExtractor {

	private final Class<? extends Annotation> annotationType;

	private final AnnotatedElement annotatedElement;

	private final Object source;

	private final Map<String, String> attributeAliasMap;


	/**
	 * Construct a new {@code AbstractAliasAwareAnnotationAttributeExtractor}.
	 * @param annotationType the annotation type to synthesize; never {@code null}
	 * @param annotatedElement the element that is annotated with the annotation
	 * of the supplied type; may be {@code null} if unknown
	 * @param source the underlying source of annotation attributes; never {@code null}
	 */
	AbstractAliasAwareAnnotationAttributeExtractor(Class<? extends Annotation> annotationType,
			AnnotatedElement annotatedElement, Object source) {
		Assert.notNull(annotationType, "annotationType must not be null");
		Assert.notNull(source, "source must not be null");
		this.annotationType = annotationType;
		this.annotatedElement = annotatedElement;
		this.source = source;
		this.attributeAliasMap = AnnotationUtils.getAttributeAliasMap(annotationType);
	}

	@Override
	public final Class<? extends Annotation> getAnnotationType() {
		return this.annotationType;
	}

	@Override
	public final AnnotatedElement getAnnotatedElement() {
		return this.annotatedElement;
	}

	@Override
	public Object getSource() {
		return this.source;
	}

	@Override
	public final Object getAttributeValue(Method attributeMethod) {
		String attributeName = attributeMethod.getName();
		Object attributeValue = getRawAttributeValue(attributeMethod);

		String aliasName = this.attributeAliasMap.get(attributeName);
		if ((aliasName != null)) {

			Object aliasValue = getRawAttributeValue(aliasName);
			Object defaultValue = AnnotationUtils.getDefaultValue(getAnnotationType(), attributeName);

			if (!nullSafeEquals(attributeValue, aliasValue) && !nullSafeEquals(attributeValue, defaultValue)
					&& !nullSafeEquals(aliasValue, defaultValue)) {
				String elementName = (getAnnotatedElement() == null ? "unknown element"
						: getAnnotatedElement().toString());
				String msg = String.format("In annotation [%s] declared on [%s] and synthesized from [%s], "
						+ "attribute [%s] and its alias [%s] are present with values of [%s] and [%s], "
						+ "but only one is permitted.", getAnnotationType().getName(), elementName, getSource(),
					attributeName, aliasName, nullSafeToString(attributeValue), nullSafeToString(aliasValue));
				throw new AnnotationConfigurationException(msg);
			}

			// If the user didn't declare the annotation with an explicit value,
			// return the value of the alias.
			if (nullSafeEquals(attributeValue, defaultValue)) {
				attributeValue = aliasValue;
			}
		}

		return attributeValue;
	}

	/**
	 * Get the raw, unmodified attribute value from the underlying
	 * {@linkplain #getSource source} that corresponds to the supplied
	 * attribute method.
	 */
	protected abstract Object getRawAttributeValue(Method attributeMethod);

	/**
	 * Get the raw, unmodified attribute value from the underlying
	 * {@linkplain #getSource source} that corresponds to the supplied
	 * attribute name.
	 */
	protected abstract Object getRawAttributeValue(String attributeName);

	private static boolean nullSafeEquals(Object o1, Object o2) {
		return ObjectUtils.nullSafeEquals(o1, o2);
	}

	private static String nullSafeToString(Object obj) {
		return ObjectUtils.nullSafeToString(obj);
	}

}
