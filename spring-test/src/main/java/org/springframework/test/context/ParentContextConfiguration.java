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

package org.springframework.test.context;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.ApplicationContext;

/**
 * {@code ParentContextConfiguration} defines class-level metadata that is used to determine how to load and configure a
 * <em>parent</em> {@link org.springframework.context.ApplicationContext ApplicationContext} for test classes.
 *
 * Test classes which have same {@link ParentContextConfiguration} configuration will share the same {@link
 * ApplicationContext} as a parent.
 *
 * @author Tadaya Tsuyukubo
 * @see ContextConfiguration
 * @since 3.2
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ParentContextConfiguration {

	/**
	 * {@link org.springframework.test.context.ContextConfiguration#value()} for the parent {@link ApplicationContext}.
	 *
	 * @see org.springframework.test.context.ContextConfiguration#value()
	 */
	String[] value() default {};

	/**
	 * {@link org.springframework.test.context.ContextConfiguration#locations()} for the parent {@link
	 * ApplicationContext}.
	 *
	 * @see org.springframework.test.context.ContextConfiguration#locations()
	 */
	String[] locations() default {};

	/**
	 * {@link org.springframework.test.context.ContextConfiguration#classes()} for the parent {@link ApplicationContext}.
	 *
	 * @see org.springframework.test.context.ContextConfiguration#classes()
	 */
	Class<?>[] classes() default {};

	/**
	 * {@link org.springframework.test.context.ContextConfiguration#inheritLocations()} for the parent {@link
	 * ApplicationContext}.
	 *
	 * @see org.springframework.test.context.ContextConfiguration#inheritLocations()
	 */
	boolean inheritLocations() default true;

	/**
	 * {@link org.springframework.test.context.ContextConfiguration#loader()} for the parent {@link ApplicationContext}.
	 *
	 * @see org.springframework.test.context.ContextConfiguration#loader()
	 */
	Class<? extends ContextLoader> loader() default ContextLoader.class;
}
