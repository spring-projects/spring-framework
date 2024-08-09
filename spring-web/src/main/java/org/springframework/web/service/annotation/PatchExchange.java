/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
	 */
	@AliasFor(annotation = HttpExchange.class)
	String[] headers() default {};

}
