/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.aot;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * {@code @DisabledInAotMode} signals that the annotated test class is <em>disabled</em>
 * in Spring AOT (ahead-of-time) mode, which means that the {@code ApplicationContext}
 * for the test class will not be processed for AOT optimizations at build time.
 *
 * <p>If a test class is annotated with {@code @DisabledInAotMode}, all other test
 * classes which specify configuration to load the same {@code ApplicationContext}
 * must also be annotated with {@code @DisabledInAotMode}. Failure to annotate
 * all such test classes will result in an exception, either at build time or
 * run time.
 *
 * <p>When used with JUnit Jupiter based tests, {@code @DisabledInAotMode} also
 * signals that the annotated test class or test method is <em>disabled</em> when
 * running the test suite in Spring AOT mode. When applied at the class level,
 * all test methods within that class will be disabled. In this sense,
 * {@code @DisabledInAotMode} has semantics similar to those of JUnit Jupiter's
 * {@link org.junit.jupiter.api.condition.DisabledInNativeImage @DisabledInNativeImage}
 * annotation.
 *
 * <p>This annotation may be used as a meta-annotation in order to create a
 * custom <em>composed annotation</em> that inherits the semantics of this
 * annotation.
 *
 * @author Sam Brannen
 * @author Stephane Nicoll
 * @since 6.1
 * @see org.springframework.aot.AotDetector#useGeneratedArtifacts() AotDetector.useGeneratedArtifacts()
 * @see org.junit.jupiter.api.condition.EnabledInNativeImage @EnabledInNativeImage
 * @see org.junit.jupiter.api.condition.DisabledInNativeImage @DisabledInNativeImage
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ExtendWith(DisabledInAotModeCondition.class)
public @interface DisabledInAotMode {

	/**
	 * Custom reason to document why the test class or test method is disabled in
	 * AOT mode.
	 * <p>If a custom reason is not supplied, the default reason will be used:
	 * {@code "Disabled in Spring AOT mode"}.
	 * <p>If a custom reason is supplied, it will be combined with the default
	 * reason. For example,
	 * {@code @DisabledInAotMode("@ContextHierarchy is not supported")} will result
	 * in a combined reason like the following:
	 * {@code "Disabled in Spring AOT mode ==> @ContextHierarchy is not supported"}.
	 * @since 6.2
	 */
	String value() default "";

}
