/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.junit.jupiter.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.core.annotation.AliasFor;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;

/**
 * {@code @SpringJUnitWebConfig} is a <em>composed annotation</em> that combines
 * {@link ExtendWith @ExtendWith(SpringExtension.class)} from JUnit Jupiter with
 * {@link ContextConfiguration @ContextConfiguration} and
 * {@link WebAppConfiguration @WebAppConfiguration} from the <em>Spring TestContext
 * Framework</em>.
 *
 * <p>This annotation will be inherited from an enclosing test class by default. See
 * {@link org.springframework.test.context.NestedTestConfiguration @NestedTestConfiguration}
 * for details.
 *
 * @author Sam Brannen
 * @since 5.0
 * @see org.junit.jupiter.api.extension.ExtendWith @ExtendWith
 * @see org.springframework.test.context.junit.jupiter.SpringExtension SpringExtension
 * @see org.springframework.test.context.junit.jupiter.SpringExtensionConfig @SpringExtensionConfig
 * @see org.springframework.test.context.junit.jupiter.SpringJUnitConfig @SpringJUnitConfig
 * @see org.springframework.test.context.ContextConfiguration @ContextConfiguration
 * @see org.springframework.test.context.web.WebAppConfiguration @WebAppConfiguration
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration
@WebAppConfiguration
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SpringJUnitWebConfig {

	/**
	 * Alias for {@link ContextConfiguration#classes}.
	 */
	@AliasFor(annotation = ContextConfiguration.class, attribute = "classes")
	Class<?>[] value() default {};

	/**
	 * Alias for {@link ContextConfiguration#classes}.
	 */
	@AliasFor(annotation = ContextConfiguration.class)
	Class<?>[] classes() default {};

	/**
	 * Alias for {@link ContextConfiguration#locations}.
	 */
	@AliasFor(annotation = ContextConfiguration.class)
	String[] locations() default {};

	/**
	 * Alias for {@link ContextConfiguration#initializers}.
	 */
	@AliasFor(annotation = ContextConfiguration.class)
	Class<? extends ApplicationContextInitializer<?>>[] initializers() default {};

	/**
	 * Alias for {@link ContextConfiguration#inheritLocations}.
	 */
	@AliasFor(annotation = ContextConfiguration.class)
	boolean inheritLocations() default true;

	/**
	 * Alias for {@link ContextConfiguration#inheritInitializers}.
	 */
	@AliasFor(annotation = ContextConfiguration.class)
	boolean inheritInitializers() default true;

	/**
	 * Alias for {@link ContextConfiguration#loader}.
	 * @since 6.1
	 */
	@AliasFor(annotation = ContextConfiguration.class)
	Class<? extends ContextLoader> loader() default ContextLoader.class;

	/**
	 * Alias for {@link ContextConfiguration#name}.
	 */
	@AliasFor(annotation = ContextConfiguration.class)
	String name() default "";

	/**
	 * Alias for {@link WebAppConfiguration#value}.
	 */
	@AliasFor(annotation = WebAppConfiguration.class, attribute = "value")
	String resourcePath() default "src/main/webapp";

}
