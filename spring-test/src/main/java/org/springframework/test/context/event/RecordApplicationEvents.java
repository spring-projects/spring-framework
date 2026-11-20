/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.event;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @RecordApplicationEvents} is an annotation that can be applied to a test
 * class to instruct the <em>Spring TestContext Framework</em> to record all
 * {@linkplain org.springframework.context.ApplicationEvent application events}
 * that are published in the {@link org.springframework.context.ApplicationContext
 * ApplicationContext} during the execution of a single test, either from the
 * test thread or its descendants.
 *
 * <p>The recorded events can be accessed via the {@link ApplicationEvents} API
 * within your tests.
 *
 * <p>This annotation may be used as a <em>meta-annotation</em> to create custom
 * <em>composed annotations</em>.
 *
 * <p>This annotation will be inherited from an enclosing test class by default. See
 * {@link org.springframework.test.context.NestedTestConfiguration @NestedTestConfiguration}
 * for details.
 *
 * @author Sam Brannen
 * @since 5.3.3
 * @see ApplicationEvents
 * @see ApplicationEventsTestExecutionListener
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface RecordApplicationEvents {
}
