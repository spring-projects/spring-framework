/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.test.context.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @WebAppConfiguration} is a class-level annotation that is used to
 * declare that the {@code ApplicationContext} loaded for an integration test
 * should be a {@link org.springframework.web.context.WebApplicationContext
 * WebApplicationContext}.
 *
 * <p>The presence of {@code @WebAppConfiguration} on a test class indicates that
 * a {@code WebApplicationContext} should be loaded for the test using a default
 * for the path to the root of the web application. To override the default,
 * specify an explicit resource path via the {@link #value} attribute.
 *
 * <p>Note that {@code @WebAppConfiguration} must be used in conjunction with
 * {@link org.springframework.test.context.ContextConfiguration @ContextConfiguration},
 * either within a single test class or within a test class hierarchy.
 *
 * <p>This annotation may be used as a <em>meta-annotation</em> to create custom
 * <em>composed annotations</em>.
 *
 * @author Sam Brannen
 * @since 3.2
 * @see org.springframework.web.context.WebApplicationContext
 * @see org.springframework.test.context.ContextConfiguration
 * @see ServletTestExecutionListener
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface WebAppConfiguration {

	/**
	 * The resource path to the root directory of the web application.
	 * <p>A path that does not include a Spring resource prefix (e.g., {@code classpath:},
	 * {@code file:}, etc.) will be interpreted as a file system resource, and a
	 * path should not end with a slash.
	 * <p>Defaults to {@code "src/main/webapp"} as a file system resource. Note
	 * that this is the standard directory for the root of a web application in
	 * a project that follows the standard Maven project layout for a WAR.
	 */
	String value() default "src/main/webapp";

}
