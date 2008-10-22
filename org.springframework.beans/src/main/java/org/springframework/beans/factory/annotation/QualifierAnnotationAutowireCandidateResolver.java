/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.beans.factory.annotation;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.SimpleTypeConverter;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.AutowireCandidateResolver;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * {@link AutowireCandidateResolver} implementation that matches bean definition
 * qualifiers against qualifier annotations on the field or parameter to be autowired.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @since 2.5
 * @see AutowireCandidateQualifier
 * @see Qualifier
 */
public class QualifierAnnotationAutowireCandidateResolver implements AutowireCandidateResolver {

	private final Set<Class<? extends Annotation>> qualifierTypes;


	/**
	 * Create a new QualifierAnnotationAutowireCandidateResolver
	 * for Spring's standard {@link Qualifier} annotation.
	 */
	public QualifierAnnotationAutowireCandidateResolver() {
		this.qualifierTypes = new HashSet<Class<? extends Annotation>>(1);
		this.qualifierTypes.add(Qualifier.class);
	}

	/**
	 * Create a new QualifierAnnotationAutowireCandidateResolver
	 * for the given qualifier annotation type.
	 * @param qualifierType the qualifier annotation to look for
	 */
	public QualifierAnnotationAutowireCandidateResolver(Class<? extends Annotation> qualifierType) {
		Assert.notNull(qualifierType, "'qualifierType' must not be null");
		this.qualifierTypes = new HashSet<Class<? extends Annotation>>(1);
		this.qualifierTypes.add(qualifierType);
	}

	/**
	 * Create a new QualifierAnnotationAutowireCandidateResolver
	 * for the given qualifier annotation types.
	 * @param qualifierTypes the qualifier annotations to look for
	 */
	public QualifierAnnotationAutowireCandidateResolver(Set<Class<? extends Annotation>> qualifierTypes) {
		Assert.notNull(qualifierTypes, "'qualifierTypes' must not be null");
		this.qualifierTypes = new HashSet<Class<? extends Annotation>>(qualifierTypes);
	}


	/**
	 * Register the given type to be used as a qualifier when autowiring.
	 * <p>This implementation only supports annotations as qualifier types.
	 * @param qualifierType the annotation type to register
	 */
	public void addQualifierType(Class<? extends Annotation> qualifierType) {
		this.qualifierTypes.add(qualifierType);
	}

	/**
	 * Determine if the provided bean definition is an autowire candidate.
	 * <p>To be considered a candidate the bean's <em>autowire-candidate</em>
	 * attribute must not have been set to 'false'. Also if an annotation on
	 * the field or parameter to be autowired is recognized by this bean factory
	 * as a <em>qualifier</em>, the bean must 'match' against the annotation as
	 * well as any attributes it may contain. The bean definition must contain
	 * the same qualifier or match by meta attributes. A "value" attribute will
	 * fallback to match against the bean name or an alias if a qualifier or
	 * attribute does not match.
	 */
	public boolean isAutowireCandidate(BeanDefinitionHolder bdHolder, DependencyDescriptor descriptor) {
		if (!bdHolder.getBeanDefinition().isAutowireCandidate()) {
			// if explicitly false, do not proceed with qualifier check
			return false;
		}
		if (descriptor == null || ObjectUtils.isEmpty(descriptor.getAnnotations())) {
			// no qualification necessary
			return true;
		}
		AbstractBeanDefinition bd = (AbstractBeanDefinition) bdHolder.getBeanDefinition();
		SimpleTypeConverter typeConverter = new SimpleTypeConverter();
		Annotation[] annotations = (Annotation[]) descriptor.getAnnotations();
		for (Annotation annotation : annotations) {
			Class<? extends Annotation> type = annotation.annotationType();
			if (isQualifier(type)) {
				AutowireCandidateQualifier qualifier = bd.getQualifier(type.getName());
				if (qualifier == null) {
					qualifier = bd.getQualifier(ClassUtils.getShortName(type));
				}
				if (qualifier == null && bd.hasBeanClass()) {
					// look for matching annotation on the target class
					Class<?> beanClass = bd.getBeanClass();
					Annotation targetAnnotation = beanClass.getAnnotation(type);
					if (targetAnnotation != null && targetAnnotation.equals(annotation)) {
						return true;
					}
				}
				Map<String, Object> attributes = AnnotationUtils.getAnnotationAttributes(annotation);
				if (attributes.isEmpty() && qualifier == null) {
					// if no attributes, the qualifier must be present
					return false;
				}
				for (Map.Entry<String, Object> entry : attributes.entrySet()) {
					String attributeName = entry.getKey();
					Object expectedValue = entry.getValue();
					Object actualValue = null;
					// check qualifier first
					if (qualifier != null) {
						actualValue = qualifier.getAttribute(attributeName);
					}
					if (actualValue == null) {
						// fall back on bean definition attribute
						actualValue = bd.getAttribute(attributeName);
					}
					if (actualValue == null && attributeName.equals(AutowireCandidateQualifier.VALUE_KEY) &&
							(expectedValue.equals(bdHolder.getBeanName()) ||
									ObjectUtils.containsElement(bdHolder.getAliases(), expectedValue))) {
						// fall back on bean name (or alias) match
						continue;
					}
					if (actualValue == null && qualifier != null) {
						// fall back on default, but only if the qualifier is present
						actualValue = AnnotationUtils.getDefaultValue(annotation, attributeName);
					}
					if (actualValue != null) {
						actualValue = typeConverter.convertIfNecessary(actualValue, expectedValue.getClass());
					}
					if (!expectedValue.equals(actualValue)) {
						return false;
					}
				}
			}
		}
		return true;
	}

	/**
	 * Checks whether the given annotation type is a recognized qualifier type.
	 */
	private boolean isQualifier(Class<? extends Annotation> annotationType) {
		for (Class<? extends Annotation> qualifierType : this.qualifierTypes) {
			if (annotationType.equals(qualifierType) || annotationType.isAnnotationPresent(qualifierType)) {
				return true;
			}
		}
		return false;
	}

}
