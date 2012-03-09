/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.core.type.filter.TypeFilter;

/**
 * Configures component scanning directives for use with @{@link Configuration} classes.
 * Provides support parallel with Spring XML's {@code <context:component-scan>} element.
 *
 * <p>One of {@link #basePackageClasses()}, {@link #basePackages()} or its alias
 * {@link #value()} must be specified.
 *
 * <p>Note that the {@code <context:component-scan>} element has an
 * {@code annotation-config} attribute, however this annotation does not. This is because
 * in almost all cases when using {@code @ComponentScan}, default annotation config
 * processing (e.g. processing {@code @Autowired} and friends) is assumed. Furthermore,
 * when using {@link AnnotationConfigApplicationContext}, annotation config processors are
 * always registered, meaning that any attempt to disable them at the
 * {@code @ComponentScan} level would be ignored.
 *
 * <p>See @{@link Configuration} Javadoc for usage examples.
 *
 * @author Chris Beams
 * @since 3.1
 * @see Configuration
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface ComponentScan {

	/**
	 * Alias for the {@link #basePackages()} attribute.
	 * Allows for more concise annotation declarations e.g.:
	 * {@code @ComponentScan("org.my.pkg")} instead of
	 * {@code @ComponentScan(basePackages="org.my.pkg")}.
	 */
	String[] value() default {};

	/**
	 * Base packages to scan for annotated components.
	 * <p>{@link #value()} is an alias for (and mutually exclusive with) this attribute.
	 * <p>Use {@link #basePackageClasses()} for a type-safe alternative to String-based package names.
	 */
	String[] basePackages() default {};

	/**
	 * Type-safe alternative to {@link #basePackages()} for specifying the packages
	 * to scan for annotated components. The package of each class specified will be scanned.
	 * <p>Consider creating a special no-op marker class or interface in each package
	 * that serves no purpose other than being referenced by this attribute.
	 */
	Class<?>[] basePackageClasses() default {};

	/**
	 * The {@link BeanNameGenerator} class to be used for naming detected components
	 * within the Spring container.
	 * <p>The default value of the {@link BeanNameGenerator} interface itself indicates
	 * that the scanner used to process this {@code @ComponentScan} annotation should
	 * use its inherited bean name generator, e.g. the default
	 * {@link AnnotationBeanNameGenerator} or any custom instance supplied to the
	 * application context at bootstrap time.
	 * @see AnnotationConfigApplicationContext#setBeanNameGenerator(BeanNameGenerator)
	 */
	Class<? extends BeanNameGenerator> nameGenerator() default BeanNameGenerator.class;

	/**
	 * The {@link ScopeMetadataResolver} to be used for resolving the scope of detected components.
	 */
	Class<? extends ScopeMetadataResolver> scopeResolver() default AnnotationScopeMetadataResolver.class;

	/**
	 * Indicates whether proxies should be generated for detected components, which may be
	 * necessary when using scopes in a proxy-style fashion.
	 * <p>The default is defer to the default behavior of the component scanner used to
	 * execute the actual scan.
	 * <p>Note that setting this attribute overrides any value set for {@link #scopeResolver()}.
	 * @see ClassPathBeanDefinitionScanner#setScopedProxyMode(ScopedProxyMode)
	 */
	ScopedProxyMode scopedProxy() default ScopedProxyMode.DEFAULT;

	/**
	 * Controls the class files eligible for component detection.
	 * <p>Consider use of {@link #includeFilters()} and {@link #excludeFilters()}
	 * for a more flexible approach.
	 */
	String resourcePattern() default ClassPathScanningCandidateComponentProvider.DEFAULT_RESOURCE_PATTERN;

	/**
	 * Indicates whether automatic detection of classes annotated with {@code @Component}
	 * {@code @Repository}, {@code @Service}, or {@code @Controller} should be enabled.
	 */
	boolean useDefaultFilters() default true;

	/**
	 * Specifies which types are eligible for component scanning.
	 * <p>Further narrows the set of candidate components from everything in
	 * {@link #basePackages()} to everything in the base packages that matches
	 * the given filter or filters.
	 * @see #resourcePattern()
	 */
	Filter[] includeFilters() default {};

	/**
	 * Specifies which types are not eligible for component scanning.
	 * @see #resourcePattern()
	 */
	Filter[] excludeFilters() default {};


	/**
	 * Declares the type filter to be used as an {@linkplain ComponentScan#includeFilters()
	 * include filter} or {@linkplain ComponentScan#excludeFilters() exclude filter}.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target({})
	@interface Filter {
		/**
		 * The type of filter to use.
		 * <p>Note that the filter types available are limited to those that may
		 * be expressed as a {@code Class} in the {@link #value()} attribute. This is
		 * in contrast to {@code <context:component-scan/>}, which allows for
		 * expression-based (i.e., string-based) filters such as AspectJ pointcuts.
		 * These filter types are intentionally not supported here, and not available
		 * in the {@link FilterType} enum.
		 * @see FilterType
		 */
		FilterType type() default FilterType.ANNOTATION;

		/**
		 * The class or classes to use as the filter. In the case of
		 * {@link FilterType#ANNOTATION}, the class will be the annotation itself. In the
		 * case of {@link FilterType#ASSIGNABLE_TYPE}, the class will be the type that
		 * detected components should be assignable to. And in the case of
		 * {@link FilterType#CUSTOM}, the class will be an implementation of
		 * {@link TypeFilter}.
		 * <p>When multiple classes are specified, OR logic is applied, e.g. "include
		 * types annotated with {@code @Foo} OR {@code @Bar}".
		 * <p>Specifying zero classes is permitted but will have no effect on component
		 * scanning.
		 */
		Class<?>[] value(); //doco
	}

}
