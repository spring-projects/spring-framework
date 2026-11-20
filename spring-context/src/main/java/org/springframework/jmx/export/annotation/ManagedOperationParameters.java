/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jmx.export.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Method-level annotation used as a container for one or more
 * {@code @ManagedOperationParameter} declarations.
 *
 * <p>Note, however, that use of the {@code @ManagedOperationParameters} container
 * is completely optional since {@code @ManagedOperationParameter} is a
 * {@linkplain java.lang.annotation.Repeatable repeatable} annotation.
 *
 * @author Rob Harrop
 * @since 1.2
 * @see org.springframework.jmx.export.metadata.ManagedOperationParameter
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ManagedOperationParameters {

	ManagedOperationParameter[] value();

}
