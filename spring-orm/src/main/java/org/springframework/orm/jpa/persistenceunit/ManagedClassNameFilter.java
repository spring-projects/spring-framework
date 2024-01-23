/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.orm.jpa.persistenceunit;

/**
 * Strategy interface to filter the list of persistent managed types to include
 * in the persistence unit. Only class names that match the filter are managed.
 *
 * @author Stephane Nicoll
 * @since 6.1.4
 * @see DefaultPersistenceUnitManager#setManagedClassNameFilter
 */
@FunctionalInterface
public interface ManagedClassNameFilter {

	/**
	 * Test if the given class name matches the filter.
	 * @param className the fully qualified class name of the persistent type to test
	 * @return {@code true} if the class name matches
	 */
	boolean matches(String className);

}
