/*
 * Copyright 2002-2013 the original author or authors.
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

/**
 * {@code @ContextHierarchy} is a class-level annotation that is used to define
 * a hierarchy of {@link org.springframework.context.ApplicationContext
 * ApplicationContexts} for integration tests.
 *
 * @author Sam Brannen
 * @since 3.2.2
 * @see ContextConfiguration
 * @see org.springframework.context.ApplicationContext
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ContextHierarchy {

	/**
	 * A list of {@link ContextConfiguration @ContextConfiguration} instances,
	 * each of which defines a level in the context hierarchy.
	 *
	 * <p>If you need to merge or override the configuration for a given level
	 * of the context hierarchy within a test class hierarchy, you must explicitly
	 * name that level by supplying the same value to the {@link ContextConfiguration#name
	 * name} attribute in {@code @ContextConfiguration} at each level in the
	 * class hierarchy.
	 */
	ContextConfiguration[] value();

}
