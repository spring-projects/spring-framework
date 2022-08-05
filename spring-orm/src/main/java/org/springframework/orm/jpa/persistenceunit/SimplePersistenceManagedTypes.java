/*
 * Copyright 2002-2022 the original author or authors.
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

import java.net.URL;
import java.util.List;

import org.springframework.lang.Nullable;

/**
 * A simple {@link PersistenceManagedTypes} implementation that holds the list
 * of managed entities.
 *
 * @author Stephane Nicoll
 * @since 6.0
 */
class SimplePersistenceManagedTypes implements PersistenceManagedTypes {

	private final List<String> managedClassNames;

	private final List<String> managedPackages;

	@Nullable
	private final URL persistenceUnitRootUrl;


	SimplePersistenceManagedTypes(List<String> managedClassNames, List<String> managedPackages,
			@Nullable URL persistenceUnitRootUrl) {
		this.managedClassNames = managedClassNames;
		this.managedPackages = managedPackages;
		this.persistenceUnitRootUrl = persistenceUnitRootUrl;
	}

	SimplePersistenceManagedTypes(List<String> managedClassNames, List<String> managedPackages) {
		this(managedClassNames, managedPackages, null);
	}

	@Override
	public List<String> getManagedClassNames() {
		return this.managedClassNames;
	}

	@Override
	public List<String> getManagedPackages() {
		return this.managedPackages;
	}

	@Override
	@Nullable
	public URL getPersistenceUnitRootUrl() {
		return this.persistenceUnitRootUrl;
	}

}
