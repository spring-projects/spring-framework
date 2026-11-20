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

import org.springframework.core.annotation.AliasFor;

/**
 * Class-level annotation that indicates to register instances of a class with a JMX server,
 * corresponding to the {@link org.springframework.jmx.export.metadata.ManagedResource} attribute.
 *
 * <p><b>Note:</b> This annotation is marked as inherited, allowing for generic
 * management-aware base classes. In such a scenario, it is recommended to
 * <i>not</i> specify an object name value since this would lead to naming
 * collisions in case of multiple subclasses getting registered.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 1.2
 * @see org.springframework.jmx.export.metadata.ManagedResource
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface ManagedResource {

	/**
	 * Alias for the {@link #objectName} attribute, for simple default usage.
	 */
	@AliasFor("objectName")
	String value() default "";

	@AliasFor("value")
	String objectName() default "";

	String description() default "";

	int currencyTimeLimit() default -1;

	boolean log() default false;

	String logFile() default "";

	String persistPolicy() default "";

	int persistPeriod() default -1;

	String persistName() default "";

	String persistLocation() default "";

}
