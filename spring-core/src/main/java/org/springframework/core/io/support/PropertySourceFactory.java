/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.core.io.support;

import java.io.IOException;

import org.springframework.core.env.PropertySource;
import org.springframework.lang.Nullable;

/**
 * Strategy interface for creating resource-based {@link PropertySource} wrappers.
 *
 * @author Juergen Hoeller
 * @since 4.3
 * @see DefaultPropertySourceFactory
 */
public interface PropertySourceFactory {

	/**
	 * Create a {@link PropertySource} that wraps the given resource.
	 * @param name the name of the property source
	 * (can be {@code null} in which case the factory implementation
	 * will have to generate a name based on the given resource)
	 * @param resource the resource (potentially encoded) to wrap
	 * @return the new {@link PropertySource} (never {@code null})
	 * @throws IOException if resource resolution failed
	 */
	PropertySource<?> createPropertySource(@Nullable String name, EncodedResource resource) throws IOException;

}
