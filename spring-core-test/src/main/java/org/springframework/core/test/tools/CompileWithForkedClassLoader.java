/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.core.test.tools;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Annotation that registers a JUnit Jupiter extension for test classes or test
 * methods that need to use a forked classloader with compiled code.
 *
 * <p>The extension allows the compiler to define classes without polluting the
 * test {@link ClassLoader}.
 *
 * <p>NOTE: this annotation cannot be used in conjunction with
 * {@link org.junit.jupiter.api.TestTemplate @TestTemplate} methods.
 * Consequently, {@link org.junit.jupiter.api.RepeatedTest @RepeatedTest} and
 * {@link org.junit.jupiter.params.ParameterizedTest @ParameterizedTest} methods
 * are not supported.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 6.0
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ExtendWith(CompileWithForkedClassLoaderExtension.class)
public @interface CompileWithForkedClassLoader {
}
