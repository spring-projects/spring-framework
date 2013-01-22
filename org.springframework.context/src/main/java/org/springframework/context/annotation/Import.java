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

/**
 * Indicates one or more @{@link Configuration} classes to import.
 *
 * <p>Provides functionality equivalent to the {@code <import/>} element in Spring XML.
 * Only supported for classes annotated with {@code @Configuration} or declaring at least
 * one {@link @Bean} method, as well as {@link ImportSelector} and
 * {@link ImportBeanDefinitionRegistrar} implementations.
 *
 * <p>@{@code Bean} definitions declared in imported {@code @Configuration} classes
 * should be accessed by using @{@link Autowired} injection.  Either the bean itself can
 * be autowired, or the configuration class instance declaring the bean can be autowired.
 * The latter approach allows for explicit, IDE-friendly navigation between
 * {@code @Configuration} class methods.
 *
 * <p>May be declared at the class level or as a meta-annotation.
 *
 * <p>If XML or other non-{@code @Configuration} bean definition resources need to be
 * imported, use @{@link ImportResource}
 *
 * @author Chris Beams
 * @since 3.0
 * @see Configuration
 * @see ImportSelector
 * @see ImportResource
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Import {

	/**
	 * The @{@link Configuration}, {@link ImportSelector} and/or
	 * {@link ImportBeanDefinitionRegistrar} classes to import.
	 */
	Class<?>[] value();
}
