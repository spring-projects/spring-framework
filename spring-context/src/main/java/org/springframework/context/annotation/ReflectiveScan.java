/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.aot.hint.annotation.Reflective;
import org.springframework.aot.hint.annotation.RegisterReflection;
import org.springframework.core.annotation.AliasFor;

/**
 * Scan arbitrary types for use of {@link Reflective}. Typically used on
 * {@link Configuration @Configuration} classes but can be added to any bean.
 * Scanning happens during AOT processing, typically at build-time.
 *
 * <p>In the example below, {@code com.example.app} and its subpackages are
 * scanned: <pre><code class="java">
 * &#064;Configuration
 * &#064;ReflectiveScan("com.example.app")
 * class MyConfiguration {
 *     // ...
 * }</code></pre>
 *
 * <p>Either {@link #basePackageClasses} or {@link #basePackages} (or its alias
 * {@link #value}) may be specified to define specific packages to scan. If specific
 * packages are not defined, scanning will occur recursively beginning with the
 * package of the class that declares this annotation.
 *
 * <p>A type does not need to be annotated at class level to be candidate, and
 * this performs a "deep scan" by loading every class in the target packages and
 * search for {@link Reflective} on types, constructors, methods, and fields.
 * Enclosed classes are candidates as well. Classes that fail to load are
 * ignored.
 *
 * @author Stephane Nicoll
 * @since 6.2
 * @see Reflective @Reflective
 * @see RegisterReflection @RegisterReflection
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface ReflectiveScan {

	/**
	 * Alias for {@link #basePackages}.
	 * <p>Allows for more concise annotation declarations if no other attributes
	 * are needed &mdash; for example, {@code @ReflectiveScan("org.my.pkg")}
	 * instead of {@code @ReflectiveScan(basePackages = "org.my.pkg")}.
	 */
	@AliasFor("basePackages")
	String[] value() default {};

	/**
	 * Base packages to scan for reflective usage.
	 * <p>{@link #value} is an alias for (and mutually exclusive with) this
	 * attribute.
	 * <p>Use {@link #basePackageClasses} for a type-safe alternative to
	 * String-based package names.
	 */
	@AliasFor("value")
	String[] basePackages() default {};

	/**
	 * Type-safe alternative to {@link #basePackages} for specifying the packages
	 * to scan for reflection usage. The package of each class specified will be scanned.
	 * <p>Consider creating a special no-op marker class or interface in each package
	 * that serves no purpose other than being referenced by this attribute.
	 */
	Class<?>[] basePackageClasses() default {};

}
