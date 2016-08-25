/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.test.context.junit.jupiter;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Disable JUnit 5(Jupiter) tests when evaluated condition returns "true"
 * that can be either case insensitive {@code String} or {@code Boolean#TRUE}.
 *
 * @author Tadaya Tsuyukubo
 * @since 5.0
 * @see SpringExtension
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DisabledIf {

	/**
	 * Alias for {@link #condition()}.
	 */
	@AliasFor("condition")
	String value() default "";

	/**
	 * Condition to disable test.
	 *
	 * <p> When case insensitive {@code String} "true" or {@code Boolean#TRUE} is returned,
	 * annotated test method or class is disabled.
	 * <p> SpEL expression can be used.
	 */
	@AliasFor("value")
	String condition() default "";

	String reason() default "";

}
