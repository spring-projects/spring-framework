package org.springframework.web.service.annotation;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * Shortcut for {@link HttpExchange @HttpExchange} for HTTP QUERY requests.
 *
 * @author Mario Ruiz
 * @since x.x.x
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@HttpExchange(method = "QUERY")
public @interface QueryExchange {
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
	 */
	@AliasFor(annotation = HttpExchange.class)
	String[] headers() default {};

	/**
	 * Alias for {@link HttpExchange#version()}.
	 */
	@AliasFor(annotation = HttpExchange.class)
	String version() default "";
}
