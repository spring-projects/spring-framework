/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.transaction.support;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;
import org.springframework.lang.Nullable;

/**
 * A simple transaction-backed {@link Scope} implementation, delegating to
 * {@link TransactionSynchronizationManager}'s resource binding mechanism.
 *
 * <p><b>NOTE:</b> Like {@link org.springframework.context.support.SimpleThreadScope},
 * this transaction scope is not registered by default in common contexts. Instead,
 * you need to explicitly assign it to a scope key in your setup, either through
 * {@link org.springframework.beans.factory.config.ConfigurableBeanFactory#registerScope}
 * or through a {@link org.springframework.beans.factory.config.CustomScopeConfigurer} bean.
 *
 * @author Juergen Hoeller
 * @since 4.2
 * @see org.springframework.context.support.SimpleThreadScope
 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory#registerScope
 * @see org.springframework.beans.factory.config.CustomScopeConfigurer
 */
public class SimpleTransactionScope implements Scope {

	@Override
	public Object get(String name, ObjectFactory<?> objectFactory) {
		ScopedObjectsHolder scopedObjects = (ScopedObjectsHolder) TransactionSynchronizationManager.getResource(this);
		if (scopedObjects == null) {
			scopedObjects = new ScopedObjectsHolder();
			TransactionSynchronizationManager.registerSynchronization(new CleanupSynchronization(scopedObjects));
			TransactionSynchronizationManager.bindResource(this, scopedObjects);
		}
		Object scopedObject = scopedObjects.scopedInstances.get(name);
		if (scopedObject == null) {
			scopedObject = objectFactory.getObject();
			scopedObjects.scopedInstances.put(name, scopedObject);
		}
		return scopedObject;
	}

	@Override
	@Nullable
	public Object remove(String name) {
		ScopedObjectsHolder scopedObjects = (ScopedObjectsHolder) TransactionSynchronizationManager.getResource(this);
		if (scopedObjects != null) {
			scopedObjects.destructionCallbacks.remove(name);
			return scopedObjects.scopedInstances.remove(name);
		}
		else {
			return null;
		}
	}

	@Override
	public void registerDestructionCallback(String name, Runnable callback) {
		ScopedObjectsHolder scopedObjects = (ScopedObjectsHolder) TransactionSynchronizationManager.getResource(this);
		if (scopedObjects != null) {
			scopedObjects.destructionCallbacks.put(name, callback);
		}
	}

	@Override
	@Nullable
	public Object resolveContextualObject(String key) {
		return null;
	}

	@Override
	@Nullable
	public String getConversationId() {
		return TransactionSynchronizationManager.getCurrentTransactionName();
	}


	/**
	 * Holder for scoped objects.
	 */
	static class ScopedObjectsHolder {

		final Map<String, Object> scopedInstances = new HashMap<>();

		final Map<String, Runnable> destructionCallbacks = new LinkedHashMap<>();
	}


	private class CleanupSynchronization extends TransactionSynchronizationAdapter {

		private final ScopedObjectsHolder scopedObjects;

		public CleanupSynchronization(ScopedObjectsHolder scopedObjects) {
			this.scopedObjects = scopedObjects;
		}

		@Override
		public void suspend() {
			TransactionSynchronizationManager.unbindResource(SimpleTransactionScope.this);
		}

		@Override
		public void resume() {
			TransactionSynchronizationManager.bindResource(SimpleTransactionScope.this, this.scopedObjects);
		}

		@Override
		public void afterCompletion(int status) {
			TransactionSynchronizationManager.unbindResourceIfPossible(SimpleTransactionScope.this);
			for (Runnable callback : this.scopedObjects.destructionCallbacks.values()) {
				callback.run();
			}
			this.scopedObjects.destructionCallbacks.clear();
			this.scopedObjects.scopedInstances.clear();
		}
	}

}
