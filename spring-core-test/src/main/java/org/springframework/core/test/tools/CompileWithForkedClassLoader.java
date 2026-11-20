/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
