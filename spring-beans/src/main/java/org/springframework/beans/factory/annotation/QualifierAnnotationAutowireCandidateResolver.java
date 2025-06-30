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

package org.springframework.beans.factory.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.SimpleTypeConverter;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.AutowireCandidateResolver;
import org.springframework.beans.factory.support.GenericTypeAwareAutowireCandidateResolver;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * {@link AutowireCandidateResolver} implementation that matches bean definition qualifiers
 * against {@link Qualifier qualifier annotations} on the field or parameter to be autowired.
 * Also supports suggested expression values through a {@link Value value} annotation.
 *
 * <p>Also supports JSR-330's {@link jakarta.inject.Qualifier} annotation if available.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 2.5
 * @see AutowireCandidateQualifier
 * @see Qualifier
 * @see Value
 */
public class QualifierAnnotationAutowireCandidateResolver extends GenericTypeAwareAutowireCandidateResolver {

	private final Set<Class<? extends Annotation>> qualifierTypes = CollectionUtils.newLinkedHashSet(2);

	private Class<? extends Annotation> valueAnnotationType = Value.class;


	/**
	 * Create a new {@code QualifierAnnotationAutowireCandidateResolver} for Spring's
	 * standard {@link Qualifier} annotation.
	 * <p>Also supports JSR-330's {@link jakarta.inject.Qualifier} annotation if available.
	 */
	@SuppressWarnings("unchecked")
	public QualifierAnnotationAutowireCandidateResolver() {
		this.qualifierTypes.add(Qualifier.class);
		try {
			this.qualifierTypes.add((Class<? extends Annotation>) ClassUtils.forName("jakarta.inject.Qualifier",
							QualifierAnnotationAutowireCandidateResolver.class.getClassLoader()));
		}
		catch (ClassNotFoundException ex) {
			// JSR-330 API (as included in Jakarta EE) not available - simply skip.
		}
	}

	/**
	 * Create a new {@code QualifierAnnotationAutowireCandidateResolver} for the given
	 * qualifier annotation type.
	 * @param qualifierType the qualifier annotation to look for
	 */
	public QualifierAnnotationAutowireCandidateResolver(Class<? extends Annotation> qualifierType) {
		Assert.notNull(qualifierType, "'qualifierType' must not be null");
		this.qualifierTypes.add(qualifierType);
	}

	/**
	 * Create a new {@code QualifierAnnotationAutowireCandidateResolver} for the given
	 * qualifier annotation types.
	 * @param qualifierTypes the qualifier annotations to look for
	 */
	public QualifierAnnotationAutowireCandidateResolver(Set<Class<? extends Annotation>> qualifierTypes) {
		Assert.notNull(qualifierTypes, "'qualifierTypes' must not be null");
		this.qualifierTypes.addAll(qualifierTypes);
	}


	/**
	 * Register the given type to be used as a qualifier when autowiring.
	 * <p>This identifies qualifier annotations for direct use (on fields,
	 * method parameters and constructor parameters) as well as meta
	 * annotations that in turn identify actual qualifier annotations.
	 * <p>This implementation only supports annotations as qualifier types.
	 * The default is Spring's {@link Qualifier} annotation which serves
	 * as a qualifier for direct use and also as a meta annotation.
	 * @param qualifierType the annotation type to register
	 */
	public void addQualifierType(Class<? extends Annotation> qualifierType) {
		this.qualifierTypes.add(qualifierType);
	}

	/**
	 * Set the 'value' annotation type, to be used on fields, method parameters
	 * and constructor parameters.
	 * <p>The default value annotation type is the Spring-provided
	 * {@link Value} annotation.
	 * <p>This setter property exists so that developers can provide their own
	 * (non-Spring-specific) annotation type to indicate a default value
	 * expression for a specific argument.
	 */
	public void setValueAnnotationType(Class<? extends Annotation> valueAnnotationType) {
		this.valueAnnotationType = valueAnnotationType;
	}


	/**
	 * Determine whether the provided bean definition is an autowire candidate.
	 * <p>To be considered a candidate the bean's <em>autowire-candidate</em>
	 * attribute must not have been set to 'false'. Also, if an annotation on
	 * the field or parameter to be autowired is recognized by this bean factory
	 * as a <em>qualifier</em>, the bean must 'match' against the annotation as
	 * well as any attributes it may contain. The bean definition must contain
	 * the same qualifier or match by meta attributes. A "value" attribute will
	 * fall back to match against the bean name or an alias if a qualifier or
	 * attribute does not match.
	 * @see Qualifier
	 */
	@Override
	public boolean isAutowireCandidate(BeanDefinitionHolder bdHolder, DependencyDescriptor descriptor) {
		if (!super.isAutowireCandidate(bdHolder, descriptor)) {
			return false;
		}
		Boolean checked = checkQualifiers(bdHolder, descriptor.getAnnotations());
		if (checked != Boolean.FALSE) {
			MethodParameter methodParam = descriptor.getMethodParameter();
			if (methodParam != null) {
				Method method = methodParam.getMethod();
				if (method == null || void.class == method.getReturnType()) {
					Boolean methodChecked = checkQualifiers(bdHolder, methodParam.getMethodAnnotations());
					if (methodChecked != null && checked == null) {
						checked = methodChecked;
					}
				}
			}
		}
		return (checked == Boolean.TRUE ||
				(checked == null && ((RootBeanDefinition) bdHolder.getBeanDefinition()).isDefaultCandidate()));
	}

	/**
	 * Match the given qualifier annotations against the candidate bean definition.
	 * @return {@code false} if a qualifier has been found but not matched,
	 * {@code true} if a qualifier has been found and matched,
	 * {@code null} if no qualifier has been found at all
	 */

	protected @Nullable Boolean checkQualifiers(BeanDefinitionHolder bdHolder, Annotation[] annotationsToSearch) {
		boolean qualifierFound = false;
		if (!ObjectUtils.isEmpty(annotationsToSearch)) {
			SimpleTypeConverter typeConverter = new SimpleTypeConverter();
			for (Annotation annotation : annotationsToSearch) {
				Class<? extends Annotation> type = annotation.annotationType();
				if (isPlainJavaAnnotation(type)) {
					continue;
				}
				boolean checkMeta = true;
				boolean fallbackToMeta = false;
				if (isQualifier(type)) {
					qualifierFound = true;
					if (!checkQualifier(bdHolder, annotation, typeConverter)) {
						fallbackToMeta = true;
					}
					else {
						checkMeta = false;
					}
				}
				if (checkMeta) {
					boolean foundMeta = false;
					for (Annotation metaAnn : type.getAnnotations()) {
						Class<? extends Annotation> metaType = metaAnn.annotationType();
						if (isPlainJavaAnnotation(metaType)) {
							continue;
						}
						if (isQualifier(metaType)) {
							qualifierFound = true;
							foundMeta = true;
							// Only accept fallback match if @Qualifier annotation has a value...
							// Otherwise, it is just a marker for a custom qualifier annotation.
							if ((fallbackToMeta && ObjectUtils.isEmpty(AnnotationUtils.getValue(metaAnn))) ||
									!checkQualifier(bdHolder, metaAnn, typeConverter)) {
								return false;
							}
						}
					}
					if (fallbackToMeta && !foundMeta) {
						return false;
					}
				}
			}
		}
		return (qualifierFound ? true : null);
	}

	/**
	 * Check whether the given annotation type is a plain "java." annotation,
	 * typically from {@code java.lang.annotation}.
	 * <p>Aligned with
	 * {@code org.springframework.core.annotation.AnnotationsScanner#hasPlainJavaAnnotationsOnly}.
	 */
	private boolean isPlainJavaAnnotation(Class<? extends Annotation> annotationType) {
		return annotationType.getName().startsWith("java.");
	}

	/**
	 * Check whether the given annotation type is a recognized qualifier type.
	 */
	protected boolean isQualifier(Class<? extends Annotation> annotationType) {
		for (Class<? extends Annotation> qualifierType : this.qualifierTypes) {
			if (annotationType.equals(qualifierType) || annotationType.isAnnotationPresent(qualifierType)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Match the given qualifier annotation against the candidate bean definition.
	 */
	protected boolean checkQualifier(
			BeanDefinitionHolder bdHolder, Annotation annotation, TypeConverter typeConverter) {

		Class<? extends Annotation> type = annotation.annotationType();
		RootBeanDefinition bd = (RootBeanDefinition) bdHolder.getBeanDefinition();

		AutowireCandidateQualifier qualifier = bd.getQualifier(type.getName());
		if (qualifier == null) {
			qualifier = bd.getQualifier(ClassUtils.getShortName(type));
		}
		if (qualifier == null) {
			// First, check annotation on qualified element, if any
			Annotation targetAnnotation = getQualifiedElementAnnotation(bd, type);
			// Then, check annotation on factory method, if applicable
			if (targetAnnotation == null) {
				targetAnnotation = getFactoryMethodAnnotation(bd, type);
			}
			if (targetAnnotation == null) {
				RootBeanDefinition dbd = getResolvedDecoratedDefinition(bd);
				if (dbd != null) {
					targetAnnotation = getFactoryMethodAnnotation(dbd, type);
				}
			}
			if (targetAnnotation == null) {
				BeanFactory beanFactory = getBeanFactory();
				// Look for matching annotation on the target class
				if (beanFactory != null) {
					try {
						Class<?> beanType = beanFactory.getType(bdHolder.getBeanName());
						if (beanType != null) {
							targetAnnotation = AnnotationUtils.getAnnotation(ClassUtils.getUserClass(beanType), type);
						}
					}
					catch (NoSuchBeanDefinitionException ex) {
						// Not the usual case - simply forget about the type check...
					}
				}
				if (targetAnnotation == null && bd.hasBeanClass()) {
					targetAnnotation = AnnotationUtils.getAnnotation(ClassUtils.getUserClass(bd.getBeanClass()), type);
				}
			}
			if (targetAnnotation != null && targetAnnotation.equals(annotation)) {
				return true;
			}
		}

		Map<String, @Nullable Object> attributes = AnnotationUtils.getAnnotationAttributes(annotation);
		if (attributes.isEmpty() && qualifier == null) {
			// If no attributes, the qualifier must be present
			return false;
		}
		for (Map.Entry<String, Object> entry : attributes.entrySet()) {
			String attributeName = entry.getKey();
			Object expectedValue = entry.getValue();
			Object actualValue = null;
			// Check qualifier first
			if (qualifier != null) {
				actualValue = qualifier.getAttribute(attributeName);
			}
			if (actualValue == null) {
				// Fall back on bean definition attribute
				actualValue = bd.getAttribute(attributeName);
			}
			if (actualValue == null && attributeName.equals(AutowireCandidateQualifier.VALUE_KEY) &&
					expectedValue instanceof String name && bdHolder.matchesName(name)) {
				// Finally, check bean name (or alias) match
				continue;
			}
			if (actualValue == null && qualifier != null) {
				// Fall back on default, but only if the qualifier is present
				actualValue = AnnotationUtils.getDefaultValue(annotation, attributeName);
			}
			if (actualValue != null) {
				actualValue = typeConverter.convertIfNecessary(actualValue, expectedValue.getClass());
			}
			if (!ObjectUtils.nullSafeEquals(expectedValue, actualValue)) {
				return false;
			}
		}
		return true;
	}

	protected @Nullable Annotation getQualifiedElementAnnotation(RootBeanDefinition bd, Class<? extends Annotation> type) {
		AnnotatedElement qualifiedElement = bd.getQualifiedElement();
		return (qualifiedElement != null ? AnnotationUtils.getAnnotation(qualifiedElement, type) : null);
	}

	protected @Nullable Annotation getFactoryMethodAnnotation(RootBeanDefinition bd, Class<? extends Annotation> type) {
		Method resolvedFactoryMethod = bd.getResolvedFactoryMethod();
		return (resolvedFactoryMethod != null ? AnnotationUtils.getAnnotation(resolvedFactoryMethod, type) : null);
	}


	/**
	 * Determine whether the given dependency declares an autowired annotation,
	 * checking its required flag.
	 * @see Autowired#required()
	 */
	@Override
	public boolean isRequired(DependencyDescriptor descriptor) {
		if (!super.isRequired(descriptor)) {
			return false;
		}
		Autowired autowired = descriptor.getAnnotation(Autowired.class);
		return (autowired == null || autowired.required());
	}

	/**
	 * Determine whether the given dependency declares a qualifier annotation.
	 * @see #isQualifier(Class)
	 * @see Qualifier
	 */
	@Override
	public boolean hasQualifier(DependencyDescriptor descriptor) {
		for (Annotation annotation : descriptor.getAnnotations()) {
			if (isQualifier(annotation.annotationType())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public @Nullable String getSuggestedName(DependencyDescriptor descriptor) {
		for (Annotation annotation : descriptor.getAnnotations()) {
			if (isQualifier(annotation.annotationType())) {
				Object value = AnnotationUtils.getValue(annotation);
				if (value instanceof String str) {
					return str;
				}
			}
		}
		return null;
	}

	/**
	 * Determine whether the given dependency declares a value annotation.
	 * @see Value
	 */
	@Override
	public @Nullable Object getSuggestedValue(DependencyDescriptor descriptor) {
		Object value = findValue(descriptor.getAnnotations());
		if (value == null) {
			MethodParameter methodParam = descriptor.getMethodParameter();
			if (methodParam != null) {
				value = findValue(methodParam.getMethodAnnotations());
			}
		}
		return value;
	}

	/**
	 * Determine a suggested value from any of the given candidate annotations.
	 */
	protected @Nullable Object findValue(Annotation[] annotationsToSearch) {
		if (annotationsToSearch.length > 0) {   // qualifier annotations have to be local
			AnnotationAttributes attr = AnnotatedElementUtils.getMergedAnnotationAttributes(
					AnnotatedElementUtils.forAnnotations(annotationsToSearch), this.valueAnnotationType);
			if (attr != null) {
				return extractValue(attr);
			}
		}
		return null;
	}

	/**
	 * Extract the value attribute from the given annotation.
	 * @since 4.3
	 */
	protected Object extractValue(AnnotationAttributes attr) {
		Object value = attr.get(AnnotationUtils.VALUE);
		if (value == null) {
			throw new IllegalStateException("Value annotation must have a value attribute");
		}
		return value;
	}

}
