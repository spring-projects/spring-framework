/*
 * Copyright 2002-2010 the original author or authors.
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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.beans.factory.support.BeanNameGenerator;


/**
 * Configures component scanning directives for use with {@link Configuration}
 * classes.  Provides support parallel with Spring XML's
 * {@code <context:component-scan>} element.
 *
 * TODO SPR-7508: complete documentation.
 *
 * @author Chris Beams
 * @since 3.1
 * @see FilterType
 * @see Configuration
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ComponentScan {

	/** base packages to scan */
	String[] value() default {};

	Class<?>[] packageOf() default Void.class;

	Class<? extends BeanNameGenerator> nameGenerator() default AnnotationBeanNameGenerator.class;

	Class<? extends ScopeMetadataResolver> scopeResolver() default AnnotationScopeMetadataResolver.class;

	String resourcePattern() default "**/*.class";

	ScopedProxyMode scopedProxy() default ScopedProxyMode.DEFAULT;

	boolean useDefaultFilters() default true;

	IncludeFilter[] includeFilters() default {};

	ExcludeFilter[] excludeFilters() default {};


	@Retention(RetentionPolicy.SOURCE)
	@interface IncludeFilter {
		FilterType type() default FilterType.ANNOTATION;
		Class<?> value();
	}

	@Retention(RetentionPolicy.SOURCE)
	@interface ExcludeFilter {
		FilterType type() default FilterType.ANNOTATION;
		Class<?> value();
	}

}
