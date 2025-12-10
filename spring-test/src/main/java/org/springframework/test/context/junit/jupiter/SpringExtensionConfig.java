/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.test.context.junit.jupiter;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @SpringExtensionConfig} is a type-level annotation that can be used to
 * configure the behavior of the {@link SpringExtension}.
 *
 * <p>This annotation is only applicable to {@link org.junit.jupiter.api.Nested @Nested}
 * test class hierarchies and should be applied to the top-level enclosing class
 * of a {@code @Nested} test class hierarchy. Consequently, there is no need to
 * declare this annotation on a test class that does not contain {@code @Nested}
 * test classes.
 *
 * <p>Note that
 * {@link org.springframework.test.context.NestedTestConfiguration @NestedTestConfiguration}
 * does not apply to this annotation: {@code @SpringExtensionConfig} will always be
 * detected within a {@code @Nested} test class hierarchy, effectively disregarding
 * any {@code @NestedTestConfiguration(OVERRIDE)} declarations.
 *
 * @author Sam Brannen
 * @since 7.0
 * @see org.springframework.test.context.junit.jupiter.SpringExtension SpringExtension
 * @see org.springframework.test.context.junit.jupiter.SpringJUnitConfig @SpringJUnitConfig
 * @see org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig @SpringJUnitWebConfig
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface SpringExtensionConfig {

	/**
	 * Specify whether the {@link SpringExtension} should use a test-class scoped
	 * {@link org.junit.jupiter.api.extension.ExtensionContext ExtensionContext}
	 * within {@link org.junit.jupiter.api.Nested @Nested} test class hierarchies.
	 *
	 * <p>By default, the {@code SpringExtension} uses a test-method scoped
	 * {@code ExtensionContext}. Thus, there is no need to declare this annotation
	 * attribute with a value of {@code false}.
	 *
	 * @see SpringExtension
	 * @see SpringExtension#getTestInstantiationExtensionContextScope(org.junit.jupiter.api.extension.ExtensionContext)
	 */
	boolean useTestClassScopedExtensionContext();

}
