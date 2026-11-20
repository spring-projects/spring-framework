/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
