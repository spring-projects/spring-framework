/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.transaction.support;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;

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
		// NOTE: Do NOT modify the following to use Map::computeIfAbsent. For details,
		// see https://github.com/spring-projects/spring-framework/issues/25801.
		Object scopedObject = scopedObjects.scopedInstances.get(name);
		if (scopedObject == null) {
			scopedObject = objectFactory.getObject();
			scopedObjects.scopedInstances.put(name, scopedObject);
		}
		return scopedObject;
	}

	@Override
	public @Nullable Object remove(String name) {
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
	public @Nullable String getConversationId() {
		return TransactionSynchronizationManager.getCurrentTransactionName();
	}


	/**
	 * Holder for scoped objects.
	 */
	static class ScopedObjectsHolder {

		final Map<String, Object> scopedInstances = new HashMap<>();

		final Map<String, Runnable> destructionCallbacks = new LinkedHashMap<>();
	}


	private class CleanupSynchronization implements TransactionSynchronization {

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
