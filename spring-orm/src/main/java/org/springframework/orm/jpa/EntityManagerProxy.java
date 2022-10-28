/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.orm.jpa;

import jakarta.persistence.EntityManager;

/**
 * Subinterface of {@link jakarta.persistence.EntityManager} to be implemented by
 * EntityManager proxies. Allows access to the underlying target EntityManager.
 *
 * <p>This interface is mainly intended for framework usage. Application code
 * should prefer the use of the {@link jakarta.persistence.EntityManager#getDelegate()}
 * method to access native functionality of the underlying resource.
 *
 * @author Juergen Hoeller
 * @since 2.5
 */
public interface EntityManagerProxy extends EntityManager {

	/**
	 * Return the underlying EntityManager that this proxy will delegate to.
	 * <p>In case of an extended EntityManager, this will be the associated
	 * raw EntityManager.
	 * <p>In case of a shared ("transactional") EntityManager, this will be
	 * the raw EntityManager that is currently associated with the transaction.
	 * Outside a transaction, an IllegalStateException will be thrown.
	 * @return the underlying raw EntityManager (never {@code null})
	 * @throws IllegalStateException if no underlying EntityManager is available
	 */
	EntityManager getTargetEntityManager() throws IllegalStateException;

}
