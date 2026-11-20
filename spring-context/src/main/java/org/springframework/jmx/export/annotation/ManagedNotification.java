/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jmx.export.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Type-level annotation that indicates a JMX notification emitted by a bean.
 *
 * <p>This annotation is a {@linkplain java.lang.annotation.Repeatable repeatable}
 * annotation.
 *
 * @author Rob Harrop
 * @since 2.0
 * @see ManagedNotifications
 * @see org.springframework.jmx.export.metadata.ManagedNotification
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Repeatable(ManagedNotifications.class)
public @interface ManagedNotification {

	String name();

	String description() default "";

	String[] notificationTypes();

}
