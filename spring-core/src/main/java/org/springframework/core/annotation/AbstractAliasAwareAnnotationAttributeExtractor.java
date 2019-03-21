/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Abstract base class for {@link AnnotationAttributeExtractor} implementations
 * that transparently enforce attribute alias semantics for annotation
 * attributes that are annotated with {@link AliasFor @AliasFor}.
 *
 * @author Sam Brannen
 * @since 4.2
 * @param <S> the type of source supported by this extractor
 * @see Annotation
 * @see AliasFor
 * @see AnnotationUtils#synthesizeAnnotation(Annotation, Object)
 */
abstract class AbstractAliasAwareAnnotationAttributeExtractor<S> implements AnnotationAttributeExtractor<S> {

	private final Class<? extends Annotation> annotationType;

	@Nullable
	private final Object annotatedElement;

	private final S source;

	private final Map<String, List<String>> attributeAliasMap;


	/**
	 * Construct a new {@code AbstractAliasAwareAnnotationAttributeExtractor}.
	 * @param annotationType the annotation type to synthesize; never {@code null}
	 * @param annotatedElement the element that is annotated with the annotation
	 * of the supplied type; may be {@code null} if unknown
	 * @param source the underlying source of annotation attributes; never {@code null}
	 */
	AbstractAliasAwareAnnotationAttributeExtractor(
			Class<? extends Annotation> annotationType, @Nullable Object annotatedElement, S source) {

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
	@Nullable
	public final Object getAnnotatedElement() {
		return this.annotatedElement;
	}

	@Override
	public final S getSource() {
		return this.source;
	}

	@Override
	@Nullable
	public final Object getAttributeValue(Method attributeMethod) {
		String attributeName = attributeMethod.getName();
		Object attributeValue = getRawAttributeValue(attributeMethod);

		List<String> aliasNames = this.attributeAliasMap.get(attributeName);
		if (aliasNames != null) {
			Object defaultValue = AnnotationUtils.getDefaultValue(this.annotationType, attributeName);
			for (String aliasName : aliasNames) {
				Object aliasValue = getRawAttributeValue(aliasName);

				if (!ObjectUtils.nullSafeEquals(attributeValue, aliasValue) &&
						!ObjectUtils.nullSafeEquals(attributeValue, defaultValue) &&
						!ObjectUtils.nullSafeEquals(aliasValue, defaultValue)) {
					String elementName = (this.annotatedElement != null ? this.annotatedElement.toString() : "unknown element");
					throw new AnnotationConfigurationException(String.format(
							"In annotation [%s] declared on %s and synthesized from [%s], attribute '%s' and its " +
							"alias '%s' are present with values of [%s] and [%s], but only one is permitted.",
							this.annotationType.getName(), elementName, this.source, attributeName, aliasName,
							ObjectUtils.nullSafeToString(attributeValue), ObjectUtils.nullSafeToString(aliasValue)));
				}

				// If the user didn't declare the annotation with an explicit value,
				// use the value of the alias instead.
				if (ObjectUtils.nullSafeEquals(attributeValue, defaultValue)) {
					attributeValue = aliasValue;
				}
			}
		}

		return attributeValue;
	}


	/**
	 * Get the raw, unmodified attribute value from the underlying
	 * {@linkplain #getSource source} that corresponds to the supplied
	 * attribute method.
	 */
	@Nullable
	protected abstract Object getRawAttributeValue(Method attributeMethod);

	/**
	 * Get the raw, unmodified attribute value from the underlying
	 * {@linkplain #getSource source} that corresponds to the supplied
	 * attribute name.
	 */
	@Nullable
	protected abstract Object getRawAttributeValue(String attributeName);

}
