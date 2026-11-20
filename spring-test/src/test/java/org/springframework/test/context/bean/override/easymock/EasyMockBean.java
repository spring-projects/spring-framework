/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.bean.override.easymock;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.easymock.MockType;

import org.springframework.core.annotation.AliasFor;
import org.springframework.test.context.bean.override.BeanOverride;

/**
 * {@code @EasyMockBean} is a field-level annotation that can be used in a test
 * class to signal that a bean should be replaced with an {@link org.easymock.EasyMock
 * EasyMock} mock.
 *
 * @author Sam Brannen
 * @since 6.2
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@BeanOverride(EasyMockBeanOverrideProcessor.class)
public @interface EasyMockBean {

	/**
	 * Alias for {@link #name}.
	 */
	@AliasFor("name")
	String value() default "";

	/**
	 * The name of the bean to mock.
	 * <p>Defaults to an empty string to denote that the name of the annotated
	 * field should be used as the bean name.
	 */
	@AliasFor("value")
	String name() default "";

	/**
	 * The {@link MockType} to use when creating the mock.
	 * <p>Defaults to {@link MockType#STRICT}.
	 */
	MockType mockType() default MockType.STRICT;

}
