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

package org.springframework.core.env;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.jspecify.annotations.Nullable;

/**
 * Holder containing one or more {@link PropertySource} objects.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 * @see PropertySource
 */
public interface PropertySources extends Iterable<PropertySource<?>> {

	/**
	 * Return a sequential {@link Stream} containing the property sources.
	 * @since 5.1
	 */
	default Stream<PropertySource<?>> stream() {
		return StreamSupport.stream(spliterator(), false);
	}

	/**
	 * Return whether a property source with the given name is contained.
	 * @param name the {@linkplain PropertySource#getName() name of the property source} to find
	 */
	boolean contains(String name);

	/**
	 * Return the property source with the given name, {@code null} if not found.
	 * @param name the {@linkplain PropertySource#getName() name of the property source} to find
	 */
	@Nullable PropertySource<?> get(String name);

}
