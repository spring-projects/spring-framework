/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.core;

/**
 * Extension of the {@link Ordered} interface, expressing a <em>priority</em>
 * ordering: order values expressed by {@code PriorityOrdered} objects
 * always apply before same order values expressed by <em>plain</em>
 * {@link Ordered} objects.
 *
 * <p>This is primarily a special-purpose interface, used for objects where
 * it is particularly important to recognize <em>prioritized</em> objects
 * first, without even obtaining the remaining objects. A typical example:
 * prioritized post-processors in a Spring
 * {@link org.springframework.context.ApplicationContext}.
 *
 * <p>Note: {@code PriorityOrdered} post-processor beans are initialized in
 * a special phase, ahead of other post-processor beans. This subtly
 * affects their autowiring behavior: they will only be autowired against
 * beans which do not require eager initialization for type matching.
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see org.springframework.beans.factory.config.PropertyOverrideConfigurer
 * @see org.springframework.beans.factory.config.PropertyPlaceholderConfigurer
 */
public interface PriorityOrdered extends Ordered {

}
