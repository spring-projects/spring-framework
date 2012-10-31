/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.orm.jpa.persistenceunit;

import javax.persistence.spi.PersistenceUnitInfo;

/**
 * Extension of the standard JPA PersistenceUnitInfo interface, for advanced collaboration
 * between Spring's {@link org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean}
 * and {@link PersistenceUnitManager} implementations.
 *
 * @author Juergen Hoeller
 * @since 3.0.1
 * @see PersistenceUnitManager
 * @see org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
 */
public interface SmartPersistenceUnitInfo extends PersistenceUnitInfo {

	/**
	 * Set the persistence provider's own package name, for exclusion from class transformation.
	 * @see #addTransformer(javax.persistence.spi.ClassTransformer)
	 * @see #getNewTempClassLoader()
	 */
	void setPersistenceProviderPackageName(String persistenceProviderPackageName);

}
