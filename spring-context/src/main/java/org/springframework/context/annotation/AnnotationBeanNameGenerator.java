/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.context.annotation;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotation.Adapt;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * {@link BeanNameGenerator} implementation for bean classes annotated with the
 * {@link org.springframework.stereotype.Component @Component} annotation or
 * with another annotation that is itself annotated with {@code @Component} as a
 * meta-annotation. For example, Spring's stereotype annotations (such as
 * {@link org.springframework.stereotype.Repository @Repository}) are
 * themselves annotated with {@code @Component}.
 *
 * <p>Also supports Jakarta EE's {@link jakarta.annotation.ManagedBean} and
 * JSR-330's {@link jakarta.inject.Named} annotations (as well as their pre-Jakarta
 * {@code javax.annotation.ManagedBean} and {@code javax.inject.Named} equivalents),
 * if available. Note that Spring component annotations always override such
 * standard annotations.
 *
 * <p>If the annotation's value doesn't indicate a bean name, an appropriate
 * name will be built based on the short name of the class (with the first
 * letter lower-cased), unless the first two letters are uppercase. For example:
 *
 * <pre class="code">com.xyz.FooServiceImpl -&gt; fooServiceImpl</pre>
 * <pre class="code">com.xyz.URLFooServiceImpl -&gt; URLFooServiceImpl</pre>
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Sam Brannen
 * @since 2.5
 * @see org.springframework.stereotype.Component#value()
 * @see org.springframework.stereotype.Repository#value()
 * @see org.springframework.stereotype.Service#value()
 * @see org.springframework.stereotype.Controller#value()
 * @see jakarta.inject.Named#value()
 * @see FullyQualifiedAnnotationBeanNameGenerator
 */
public class AnnotationBeanNameGenerator implements BeanNameGenerator {

	/**
	 * A convenient constant for a default {@code AnnotationBeanNameGenerator} instance,
	 * as used for component scanning purposes.
	 * @since 5.2
	 */
	public static final AnnotationBeanNameGenerator INSTANCE = new AnnotationBeanNameGenerator();

	private static final String COMPONENT_ANNOTATION_CLASSNAME = "org.springframework.stereotype.Component";

	private static final Adapt[] ADAPTATIONS = Adapt.values(false, true);


	private static final Log logger = LogFactory.getLog(AnnotationBeanNameGenerator.class);

	/**
	 * Set used to track which stereotype annotations have already been checked
	 * to see if they use a convention-based override for the {@code value}
	 * attribute in {@code @Component}.
	 * @since 6.1
	 * @see #determineBeanNameFromAnnotation(AnnotatedBeanDefinition)
	 */
	private static final Set<String> conventionBasedStereotypeCheckCache = ConcurrentHashMap.newKeySet();

	private final Map<String, Set<String>> metaAnnotationTypesCache = new ConcurrentHashMap<>();



	@Override
	public String generateBeanName(BeanDefinition definition, BeanDefinitionRegistry registry) {
		if (definition instanceof AnnotatedBeanDefinition annotatedBeanDefinition) {
			String beanName = determineBeanNameFromAnnotation(annotatedBeanDefinition);
			if (StringUtils.hasText(beanName)) {
				// Explicit bean name found.
				return beanName;
			}
		}
		// Fallback: generate a unique default bean name.
		return buildDefaultBeanName(definition, registry);
	}

	/**
	 * Derive a bean name from one of the annotations on the class.
	 * @param annotatedDef the annotation-aware bean definition
	 * @return the bean name, or {@code null} if none is found
	 */
	@Nullable
	protected String determineBeanNameFromAnnotation(AnnotatedBeanDefinition annotatedDef) {
		AnnotationMetadata metadata = annotatedDef.getMetadata();

		String beanName = getExplicitBeanName(metadata);
		if (beanName != null) {
			return beanName;
		}

		// List of annotations directly present on the class we're searching on.
		// MergedAnnotation implementations do not implement equals()/hashCode(),
		// so we use a List and a 'visited' Set below.
		List<MergedAnnotation<Annotation>> mergedAnnotations = metadata.getAnnotations().stream()
				.filter(MergedAnnotation::isDirectlyPresent)
				.toList();

		Set<AnnotationAttributes> visited = new HashSet<>();

		for (MergedAnnotation<Annotation> mergedAnnotation : mergedAnnotations) {
			AnnotationAttributes attributes = mergedAnnotation.asAnnotationAttributes(ADAPTATIONS);
			if (visited.add(attributes)) {
				String annotationType = mergedAnnotation.getType().getName();
				Set<String> metaAnnotationTypes = this.metaAnnotationTypesCache.computeIfAbsent(annotationType,
						key -> getMetaAnnotationTypes(mergedAnnotation));
				if (isStereotypeWithNameValue(annotationType, metaAnnotationTypes, attributes)) {
					Object value = attributes.get("value");
					if (value instanceof String currentName && !currentName.isBlank()) {
						if (conventionBasedStereotypeCheckCache.add(annotationType) &&
								metaAnnotationTypes.contains(COMPONENT_ANNOTATION_CLASSNAME) && logger.isWarnEnabled()) {
							logger.warn("""
									Support for convention-based stereotype names is deprecated and will \
									be removed in a future version of the framework. Please annotate the \
									'value' attribute in @%s with @AliasFor(annotation=Component.class) \
									to declare an explicit alias for @Component's 'value' attribute."""
										.formatted(annotationType));
						}
						if (beanName != null && !currentName.equals(beanName)) {
							throw new IllegalStateException("Stereotype annotations suggest inconsistent " +
									"component names: '" + beanName + "' versus '" + currentName + "'");
						}
						beanName = currentName;
					}
				}
			}
		}
		return beanName;
	}

	private Set<String> getMetaAnnotationTypes(MergedAnnotation<Annotation> mergedAnnotation) {
		Set<String> result = MergedAnnotations.from(mergedAnnotation.getType()).stream()
				.map(metaAnnotation -> metaAnnotation.getType().getName())
				.collect(Collectors.toCollection(LinkedHashSet::new));
		return (result.isEmpty() ? Collections.emptySet() : result);
	}

	/**
	 * Get the explicit bean name for the underlying class, as configured via
	 * {@link org.springframework.stereotype.Component @Component} and taking into
	 * account {@link org.springframework.core.annotation.AliasFor @AliasFor}
	 * semantics for annotation attribute overrides for {@code @Component}'s
	 * {@code value} attribute.
	 * @param metadata the {@link AnnotationMetadata} for the underlying class
	 * @return the explicit bean name, or {@code null} if not found
	 * @since 6.1
	 * @see org.springframework.stereotype.Component#value()
	 */
	@Nullable
	private String getExplicitBeanName(AnnotationMetadata metadata) {
		List<String> names = metadata.getAnnotations().stream(COMPONENT_ANNOTATION_CLASSNAME)
				.map(annotation -> annotation.getString(MergedAnnotation.VALUE))
				.filter(StringUtils::hasText)
				.map(String::trim)
				.distinct()
				.toList();

		if (names.size() == 1) {
			return names.get(0);
		}
		if (names.size() > 1) {
			throw new IllegalStateException(
					"Stereotype annotations suggest inconsistent component names: " + names);
		}
		return null;
	}

	/**
	 * Check whether the given annotation is a stereotype that is allowed
	 * to suggest a component name through its {@code value()} attribute.
	 * @param annotationType the name of the annotation class to check
	 * @param metaAnnotationTypes the names of meta-annotations on the given annotation
	 * @param attributes the map of attributes for the given annotation
	 * @return whether the annotation qualifies as a stereotype with component name
	 */
	protected boolean isStereotypeWithNameValue(String annotationType,
			Set<String> metaAnnotationTypes, Map<String, Object> attributes) {

		boolean isStereotype = metaAnnotationTypes.contains(COMPONENT_ANNOTATION_CLASSNAME) ||
				annotationType.equals("jakarta.annotation.ManagedBean") ||
				annotationType.equals("javax.annotation.ManagedBean") ||
				annotationType.equals("jakarta.inject.Named") ||
				annotationType.equals("javax.inject.Named");

		return (isStereotype && attributes.containsKey("value"));
	}

	/**
	 * Derive a default bean name from the given bean definition.
	 * <p>The default implementation delegates to {@link #buildDefaultBeanName(BeanDefinition)}.
	 * @param definition the bean definition to build a bean name for
	 * @param registry the registry that the given bean definition is being registered with
	 * @return the default bean name (never {@code null})
	 */
	protected String buildDefaultBeanName(BeanDefinition definition, BeanDefinitionRegistry registry) {
		return buildDefaultBeanName(definition);
	}

	/**
	 * Derive a default bean name from the given bean definition.
	 * <p>The default implementation simply builds a decapitalized version
	 * of the short class name: e.g. "mypackage.MyJdbcDao" &rarr; "myJdbcDao".
	 * <p>Note that inner classes will thus have names of the form
	 * "outerClassName.InnerClassName", which because of the period in the
	 * name may be an issue if you are autowiring by name.
	 * @param definition the bean definition to build a bean name for
	 * @return the default bean name (never {@code null})
	 */
	protected String buildDefaultBeanName(BeanDefinition definition) {
		String beanClassName = definition.getBeanClassName();
		Assert.state(beanClassName != null, "No bean class name set");
		String shortClassName = ClassUtils.getShortName(beanClassName);
		return StringUtils.uncapitalizeAsProperty(shortClassName);
	}

}
