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
 * Method-level annotation that indicates to expose a given bean property as a
 * JMX attribute, corresponding to the
 * {@link org.springframework.jmx.export.metadata.ManagedAttribute}.
 *
 * <p>Only valid when used on a JavaBean getter or setter.
 *
 * @author Rob Harrop
 * @since 1.2
 * @see org.springframework.jmx.export.metadata.ManagedAttribute
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ManagedAttribute {

	/**
	 * Set the default value for the attribute in a {@link javax.management.Descriptor}.
	 */
	String defaultValue() default "";

	/**
	 * Set the description for the attribute in a {@link javax.management.Descriptor}.
	 */
	String description() default "";

	/**
	 * Set the currency time limit field in a {@link javax.management.Descriptor}.
	 */
	int currencyTimeLimit() default -1;

	/**
	 * Set the persistPolicy field in a {@link javax.management.Descriptor}.
	 */
	String persistPolicy() default "";

	/**
	 * Set the persistPeriod field in a {@link javax.management.Descriptor}.
	 */
	int persistPeriod() default -1;

}
