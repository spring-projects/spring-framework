/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.bind.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.stereotype.Component;

/**
 * Indicates the annotated class assists a "Controller".
 *
 * <p>Serves as a specialization of {@link Component @Component}, allowing for
 * implementation classes to be autodetected through classpath scanning.
 *
 * <p>It is typically used to define {@link ExceptionHandler @ExceptionHandler},
 * {@link InitBinder @InitBinder}, and {@link ModelAttribute @ModelAttribute}
 * methods that apply to all {@link RequestMapping @RequestMapping} methods.
 *
 * <p>One of {@link #annotations()}, {@link #basePackageClasses()},
 * {@link #basePackages()} or its alias {@link #value()}
 * may be specified to define specific subsets of Controllers
 * to assist. When multiple selectors are applied, OR logic is applied -
 * meaning selected Controllers should match at least one selector.
 *
 * <p>The default behavior (i.e. if used without any selector),
 * the {@code @ControllerAdvice} annotated class will
 * assist all known Controllers.
 *
 * <p>Note that those checks are done at runtime, so adding many attributes and using
 * multiple strategies may have negative impacts (complexity, performance).
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 3.2
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface ControllerAdvice {

	/**
	 * Alias for the {@link #basePackages()} attribute.
	 * Allows for more concise annotation declarations e.g.:
	 * {@code @ControllerAdvice("org.my.pkg")} instead of
	 * {@code @ControllerAdvice(basePackages="org.my.pkg")}.
	 * @since 4.0
	 */
	String[] value() default {};

	/**
	 * Array of base packages.
	 * Controllers that belong to those base packages will be selected
	 * to be assisted by the annotated class, e.g.:
	 * {@code @ControllerAdvice(basePackages="org.my.pkg")}
	 * {@code @ControllerAdvice(basePackages={"org.my.pkg","org.my.other.pkg"})}
	 *
	 * <p>{@link #value()} is an alias for this attribute.
	 * <p>Use {@link #basePackageClasses()} for a type-safe alternative to String-based package names.
	 * @since 4.0
	 */
	String[] basePackages() default {};

	/**
	 * Array of classes.
	 * Controllers that are assignable to at least one of the given types
	 * will be assisted by the {@code @ControllerAdvice} annotated class.
	 * @since 4.0
	 */
	Class<?>[] assignableTypes() default {};


	/**
	 * Array of annotations.
	 * Controllers that are annotated with this/one of those annotation(s)
	 * will be assisted by the {@code @ControllerAdvice} annotated class.
	 *
	 * <p>Consider creating a special annotation or use a predefined one,
	 * like {@link RestController @RestController}.
	 * @since 4.0
	 */
	Class<?>[] annotations() default {};

	/**
	 * Type-safe alternative to {@link #value()} for specifying the packages
	 * to select Controllers to be assisted by the {@code @ControllerAdvice}
	 * annotated class.
	 *
	 * <p>Consider creating a special no-op marker class or interface in each package
	 * that serves no purpose other than being referenced by this attribute.
	 * @since 4.0
	 */
	Class<?>[] basePackageClasses() default {};
}
