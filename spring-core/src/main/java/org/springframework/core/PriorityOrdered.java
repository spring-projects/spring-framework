/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core;

/**
 * Extension of the {@link Ordered} interface, expressing a <em>priority</em>
 * ordering: {@code PriorityOrdered} objects are always applied before
 * <em>plain</em> {@link Ordered} objects regardless of their order values.
 *
 * <p>When sorting a set of {@code Ordered} objects, {@code PriorityOrdered}
 * objects and <em>plain</em> {@code Ordered} objects are effectively treated as
 * two separate subsets, with the set of {@code PriorityOrdered} objects preceding
 * the set of <em>plain</em> {@code Ordered} objects and with relative
 * ordering applied within those subsets.
 *
 * <p>This is primarily a special-purpose interface, used within the framework
 * itself for objects where it is particularly important to recognize
 * <em>prioritized</em> objects first, potentially without even obtaining the
 * remaining objects. A typical example: prioritized post-processors in a Spring
 * {@link org.springframework.context.ApplicationContext}.
 *
 * <p>Note: {@code PriorityOrdered} post-processor beans are initialized in
 * a special phase, ahead of other post-processor beans. This subtly
 * affects their autowiring behavior: they will only be autowired against
 * beans which do not require eager initialization for type matching.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 2.5
 * @see org.springframework.beans.factory.config.PropertyOverrideConfigurer
 */
public interface PriorityOrdered extends Ordered {
}
