/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as being eligible for Spring-driven configuration.
 *
 * <p>Typically used with the AspectJ {@code AnnotationBeanConfigurerAspect}.
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Adrian Colyer
 * @author Ramnivas Laddad
 * @since 2.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Configurable {

	/**
	 * The name of the bean definition that serves as the configuration template.
	 */
	String value() default "";

	/**
	 * Are dependencies to be injected via autowiring?
	 */
	Autowire autowire() default Autowire.NO;

	/**
	 * Is dependency checking to be performed for configured objects?
	 */
	boolean dependencyCheck() default false;

	/**
	 * Are dependencies to be injected prior to the construction of an object?
	 */
	boolean preConstruction() default false;

}
