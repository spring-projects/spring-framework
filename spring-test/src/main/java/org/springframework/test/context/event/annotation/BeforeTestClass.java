/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.event.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AliasFor;
import org.springframework.test.context.event.BeforeTestClassEvent;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * {@link EventListener @EventListener} annotation used to consume a
 * {@link BeforeTestClassEvent} published by the
 * {@link org.springframework.test.context.event.EventPublishingTestExecutionListener
 * EventPublishingTestExecutionListener}.
 *
 * <p>This annotation may be used on {@code @EventListener}-compliant methods within
 * a Spring test {@link org.springframework.context.ApplicationContext ApplicationContext}
 * &mdash; for example, on methods in a
 * {@link org.springframework.context.annotation.Configuration @Configuration}
 * class. A method annotated with this annotation will be invoked as part of the
 * {@link org.springframework.test.context.TestExecutionListener#beforeTestClass}
 * lifecycle.
 *
 * <p>Event processing can optionally be made {@linkplain #value conditional} via
 * a SpEL expression &mdash; for example,
 * {@code @BeforeTestClass("event.testContext.testClass.name matches '.+IntegrationTests'")}.
 *
 * <p>The {@code EventPublishingTestExecutionListener} must be registered in order
 * for this annotation to have an effect &mdash; for example, via
 * {@link org.springframework.test.context.TestExecutionListeners @TestExecutionListeners}.
 * Note, however, that the {@code EventPublishingTestExecutionListener} is registered
 * by default.
 *
 * @author Frank Scheffler
 * @author Sam Brannen
 * @since 5.2
 * @see BeforeTestClassEvent
 */
@Retention(RUNTIME)
@Target({ METHOD, ANNOTATION_TYPE })
@Documented
@EventListener(BeforeTestClassEvent.class)
public @interface BeforeTestClass {

	/**
	 * Alias for {@link EventListener#condition}.
	 */
	@AliasFor(annotation = EventListener.class, attribute = "condition")
	String value() default "";

}
