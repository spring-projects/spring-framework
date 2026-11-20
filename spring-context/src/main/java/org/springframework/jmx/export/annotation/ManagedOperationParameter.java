/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jmx.export.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Method-level annotation used to provide metadata about operation parameters, corresponding
 * to a {@link org.springframework.jmx.export.metadata.ManagedOperationParameter} attribute.
 *
 * <p>This annotation is a {@linkplain java.lang.annotation.Repeatable repeatable}
 * annotation.
 *
 * @author Rob Harrop
 * @since 1.2
 * @see ManagedOperationParameters
 * @see org.springframework.jmx.export.metadata.ManagedOperationParameter
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(ManagedOperationParameters.class)
public @interface ManagedOperationParameter {

	String name();

	String description();

}
