/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.service.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

/**
 * Shortcut for {@link HttpExchange @HttpExchange} for HTTP PATCH requests.
 *
 * @author Rossen Stoyanchev
 * @since 6.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@HttpExchange(method = "PATCH")
public @interface PatchExchange {

	/**
	 * Alias for {@link HttpExchange#value}.
	 */
	@AliasFor(annotation = HttpExchange.class)
	String value() default "";

	/**
	 * Alias for {@link HttpExchange#url()}.
	 */
	@AliasFor(annotation = HttpExchange.class)
	String url() default "";

	/**
	 * Alias for {@link HttpExchange#contentType()}.
	 */
	@AliasFor(annotation = HttpExchange.class)
	String contentType() default "";

	/**
	 * Alias for {@link HttpExchange#accept()}.
	 */
	@AliasFor(annotation = HttpExchange.class)
	String[] accept() default {};

	/**
	 * Alias for {@link HttpExchange#headers()}.
	 * @since 6.2
	 */
	@AliasFor(annotation = HttpExchange.class)
	String[] headers() default {};

	/**
	 * Alias for {@link HttpExchange#version()}.
	 * @since 7.0
	 */
	@AliasFor(annotation = HttpExchange.class)
	String version() default "";

}
