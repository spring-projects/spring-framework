/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.bind.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

/**
 * A shortcut annotation that combines {@link ControllerAdvice @ControllerAdvice}
 * with {@link ResponseBody @ResponseBody}, in effect simply an
 * {@code @ControllerAdvice} whose exception handler methods render to the
 * response body.
 *
 * <p>By default, {@code @RestControllerAdvice} applies to any controller,
 * including {@code @Controller} and {@code @RestController}. Use attributes of
 * the annotation to apply more specific filtering criteria.
 *
 * <p>See {@link ControllerAdvice} for more details.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 4.3
 * @see RestController
 * @see ControllerAdvice
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ControllerAdvice
@ResponseBody
public @interface RestControllerAdvice {

	/**
	 * Alias for {@link ControllerAdvice#name}.
	 * @since 6.1
	 */
	@AliasFor(annotation = ControllerAdvice.class)
	String name() default "";

	/**
	 * Alias for the {@link #basePackages} attribute.
	 * <p>Allows for more concise annotation declarations &mdash; for example,
	 * {@code @RestControllerAdvice("org.my.pkg")} is equivalent to
	 * {@code @RestControllerAdvice(basePackages = "org.my.pkg")}.
	 * @see #basePackages
	 */
	@AliasFor(annotation = ControllerAdvice.class)
	String[] value() default {};

	/**
	 * Array of base packages.
	 * <p>Controllers that belong to those base packages or sub-packages thereof
	 * will be included &mdash; for example,
	 * {@code @RestControllerAdvice(basePackages = "org.my.pkg")} or
	 * {@code @RestControllerAdvice(basePackages = {"org.my.pkg", "org.my.other.pkg"})}.
	 * <p>{@link #value} is an alias for this attribute, simply allowing for
	 * more concise use of the annotation.
	 * <p>Also consider using {@link #basePackageClasses} as a type-safe
	 * alternative to String-based package names.
	 */
	@AliasFor(annotation = ControllerAdvice.class)
	String[] basePackages() default {};

	/**
	 * Type-safe alternative to {@link #basePackages} for specifying the packages
	 * in which to select controllers to be advised by the {@code @RestControllerAdvice}
	 * annotated class.
	 * <p>Consider creating a special no-op marker class or interface in each package
	 * that serves no purpose other than being referenced by this attribute.
	 */
	@AliasFor(annotation = ControllerAdvice.class)
	Class<?>[] basePackageClasses() default {};

	/**
	 * Array of classes.
	 * <p>Controllers that are assignable to at least one of the given types
	 * will be advised by the {@code @RestControllerAdvice} annotated class.
	 */
	@AliasFor(annotation = ControllerAdvice.class)
	Class<?>[] assignableTypes() default {};

	/**
	 * Array of annotations.
	 * <p>Controllers that are annotated with at least one of the supplied annotation
	 * types will be advised by the {@code @RestControllerAdvice} annotated class.
	 * <p>Consider creating a custom composed annotation or use a predefined one,
	 * like {@link RestController @RestController}.
	 */
	@AliasFor(annotation = ControllerAdvice.class)
	Class<? extends Annotation>[] annotations() default {};

}
