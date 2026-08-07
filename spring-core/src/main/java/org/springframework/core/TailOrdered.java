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

package org.springframework.core;

/**
 * Extension of the {@link Ordered} interface, expressing a <em>tail</em>
 * ordering: {@code TailOrdered} objects are always applied after
 * <em>plain</em> {@link Ordered} objects regardless of their order values.
 *
 * <p>When sorting a set of {@code Ordered} objects, {@code TailOrdered}
 * objects and <em>plain</em> {@code Ordered} objects are effectively treated as
 * two separate subsets, with the set of {@code TailOrdered} objects following
 * the set of <em>plain</em> {@code Ordered} objects and with relative
 * ordering applied within those subsets.
 *
 * @author Yanming Zhou
 * @since 7.1
 * @see PriorityOrdered
 */
public interface TailOrdered extends Ordered {
}
