/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.transaction;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Test annotation which indicates that the annotated {@code void} method
 * should be executed <em>after</em> a transaction is ended for a test method
 * configured to run within a transaction via Spring's {@code @Transactional}
 * annotation.
 *
 * <p>Generally speaking, {@code @AfterTransaction} methods must not accept any
 * arguments. However, for tests using the
 * {@link org.springframework.test.context.junit.jupiter.SpringExtension SpringExtension}
 * with JUnit Jupiter, {@code @AfterTransaction} methods may optionally accept
 * arguments which will be resolved by any registered JUnit
 * {@link org.junit.jupiter.api.extension.ParameterResolver ParameterResolver}
 * extension such as the {@code SpringExtension}. This means that JUnit-specific
 * arguments like {@link org.junit.jupiter.api.TestInfo TestInfo} or beans from
 * the test's {@code ApplicationContext} may be provided to {@code @AfterTransaction}
 * methods analogous to {@code @AfterEach} methods.
 *
 * <p>{@code @AfterTransaction} methods declared in superclasses or as interface
 * default methods will be executed after those of the current test class.
 *
 * <p>This annotation may be used as a <em>meta-annotation</em> to create custom
 * <em>composed annotations</em>.
 *
 * @author Sam Brannen
 * @since 2.5
 * @see org.springframework.transaction.annotation.Transactional
 * @see BeforeTransaction
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AfterTransaction {
}
