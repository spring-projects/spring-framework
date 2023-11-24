/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.servlet.handler;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Annotation for tests parameterized to use either
 * {@link org.springframework.web.util.pattern.PathPatternParser} or
 * {@link org.springframework.util.PathMatcher} for URL pattern matching.
 *
 * @author Rossen Stoyanchev
 * @since 5.3
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
// Do not auto-close arguments since ConfigurableWebApplicationContext implements
// AutoCloseable and is shared between parameterized test invocations.
@ParameterizedTest(name = "[{index}] {0}", autoCloseArguments = false)
@MethodSource("pathPatternsArguments")
public @interface PathPatternsParameterizedTest {
}
