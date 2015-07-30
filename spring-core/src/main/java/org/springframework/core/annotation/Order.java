/*
 * Copyright 2002-2015 the original author or authors.
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
 * <p>The {@link #value} is optional and represents an order value as defined
 * in the {@link Ordered} interface. Lower values have higher priority. The
 * default value is {@code Ordered.LOWEST_PRECEDENCE}, indicating
 * lowest priority (losing to any other specified order value).
 *
 * <p>Since Spring 4.1, the standard {@link javax.annotation.Priority}
 * annotation can be used as a drop-in replacement for this annotation.
 *
 * <p><b>NOTE</b>: Annotation-based ordering is supported for specific kinds
 * of components only &mdash; for example, for annotation-based AspectJ
 * aspects. Ordering strategies within the Spring container, on the other
 * hand, are typically based on the {@link Ordered} interface in order to
 * allow for programmatically configurable ordering of each <i>instance</i>.
 *
 * <p>Consult the Javadoc for {@link org.springframework.core.OrderComparator
 * OrderComparator} for details on the sort semantics for non-ordered objects.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 * @see org.springframework.core.Ordered
 * @see AnnotationAwareOrderComparator
 * @see OrderUtils
 * @see javax.annotation.Priority
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
