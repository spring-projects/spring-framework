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

package org.springframework.test.context.support;

import org.springframework.lang.Nullable;

/**
 * Strategy for providing named properties &mdash; for example, for looking up
 * key-value pairs in a generic fashion.
 *
 * <p>Primarily intended for use within the framework.
 *
 * @author Sam Brannen
 * @since 5.3
 */
@FunctionalInterface
public interface PropertyProvider {

	/**
	 * Get the value of the named property.
	 *
	 * @param name the name of the property to retrieve
	 * @return the value of the property or {@code null} if not found
	 */
	@Nullable
	String get(String name);

}
