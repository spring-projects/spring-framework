/*
 * Copyright 2002-2019 the original author or authors.
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

import java.lang.annotation.Annotation;
import java.util.function.Supplier;

/**
 * Common interface for annotation config application contexts,
 * defining {@link #register} and {@link #scan} methods.
 *
 * @author Juergen Hoeller
 * @since 4.1
 */
public interface AnnotationConfigRegistry {

	/**
	 * Register one or more annotated classes to be processed.
	 * <p>Calls to {@code register} are idempotent; adding the same
	 * annotated class more than once has no additional effect.
	 * @param annotatedClasses one or more annotated classes,
	 * e.g. {@link Configuration @Configuration} classes
	 */
	void register(Class<?>... annotatedClasses);

	/**
	 * Perform a scan within the specified base packages.
	 * @param basePackages the packages to check for annotated classes
	 */
	void scan(String... basePackages);

	/**
	 * Register a bean from the given bean class, deriving its metadata from
	 * class-declared annotations.
	 * @param annotatedClass the class of the bean
	 * @param qualifiers specific qualifier annotations to consider,
	 * in addition to qualifiers at the bean class level (may be empty).
	 * These can be actual autowire qualifiers as well as {@link Primary}
	 * and {@link Lazy}.
	 * @since 5.2
	 */
	@SuppressWarnings("unchecked")
	<T> void registerBean(Class<T> annotatedClass, Class<? extends Annotation>... qualifiers);

	/**
	 * Register a bean from the given bean class, using the given supplier for
	 * obtaining a new instance (typically declared as a lambda expression or
	 * method reference).
	 * @param annotatedClass the class of the bean
	 * @param supplier a callback for creating an instance of the bean
	 * @param qualifiers specific qualifier annotations to consider,
	 * in addition to qualifiers at the bean class level (may be empty).
	 * These can be actual autowire qualifiers as well as {@link Primary}
	 * and {@link Lazy}.
	 * @since 5.2
	 */
	@SuppressWarnings("unchecked")
	<T> void registerBean(Class<T> annotatedClass, Supplier<T> supplier, Class<? extends Annotation>... qualifiers);

}
