/*
 * Copyright 2002-2009 the original author or authors.
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
 * Indicates one or more {@link Configuration} classes to import.
 *
 * <p>Provides functionality equivalent to the {@literal <import/>} element in Spring XML.
 * Only supported for actual {@literal @Configuration}-annotated classes.
 *
 * <p>{@literal @Bean} definitions declared in imported {@literal @Configuration} classes
 * should be accessed by using {@link Autowired @Autowired} injection.  Either the bean
 * itself can be autowired, or the configuration class instance declaring the bean can be
 * autowired.  The latter approach allows for explicit, IDE-friendly navigation between
 * {@literal @Configuration} class methods.
 *
 * <p>If XML or other non-{@literal @Configuration} bean definition resources need to be
 * imported, use {@link ImportResource @ImportResource}
 *
 * @author Chris Beams
 * @since 3.0
 * @see Configuration
 * @see ImportResource
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Import {

	/**
	 * The {@link Configuration} class or classes to import.
	 */
	Class<?>[] value();
}
