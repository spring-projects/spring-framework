/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jmx.export.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Type-level annotation used as a container for one or more
 * {@code @ManagedNotification} declarations.
 *
 * <p>Note, however, that use of the {@code @ManagedNotifications} container
 * is completely optional since {@code @ManagedNotification} is a
 * {@linkplain java.lang.annotation.Repeatable repeatable} annotation.
 *
 * @author Rob Harrop
 * @since 2.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface ManagedNotifications {

	ManagedNotification[] value();

}
