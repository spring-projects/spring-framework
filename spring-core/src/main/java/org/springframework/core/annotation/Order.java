/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.Ordered;

/**
 * {@code @Order} defines the sort order for an annotated component.
 *
 * <p>The {@link #value} is optional and represents an order value as defined in the
 * {@link Ordered} interface. Lower values have higher priority. The default value is
 * {@code Ordered.LOWEST_PRECEDENCE}, indicating the lowest priority (losing to any
 * other specified order value).
 *
 * <p><b>NOTE:</b> Since Spring 4.0, annotation-based ordering is supported for many
 * kinds of components in Spring, even for collection injection where the order values
 * of the target components are taken into account (either from their target class or
 * from their {@code @Bean} method). While such order values may influence priorities
 * at injection points, please be aware that they do not influence singleton startup
 * order which is an orthogonal concern determined by dependency relationships and
 * {@code @DependsOn} declarations (influencing a runtime-determined dependency graph).
 *
 * <p>Since Spring 4.1, the standard {@link jakarta.annotation.Priority} annotation
 * can be used as a drop-in replacement for this annotation in ordering scenarios.
 * Note that {@code @Priority} may have additional semantics when a single element
 * has to be picked (see {@link AnnotationAwareOrderComparator#getPriority}).
 *
 * <p>Alternatively, order values may also be determined on a per-instance basis
 * through the {@link Ordered} interface, allowing for configuration-determined
 * instance values instead of hard-coded values attached to a particular class.
 *
 * <p>Consult the javadoc for {@link org.springframework.core.OrderComparator
 * OrderComparator} for details on the sort semantics for non-ordered objects.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 * @see org.springframework.core.Ordered
 * @see AnnotationAwareOrderComparator
 * @see OrderUtils
 * @see jakarta.annotation.Priority
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
@Documented
public @interface Order {

	/**
	 * The order value.
	 * <p>Default is {@link Ordered#LOWEST_PRECEDENCE}.
	 * @see Ordered#getOrder()
	 */
	int value() default Ordered.LOWEST_PRECEDENCE;

}
