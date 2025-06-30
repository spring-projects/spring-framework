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

package org.springframework.orm;

import org.jspecify.annotations.Nullable;

import org.springframework.dao.OptimisticLockingFailureException;

/**
 * Exception thrown on an optimistic locking violation for a mapped object.
 * Provides information about the persistent class and the identifier.
 *
 * @author Juergen Hoeller
 * @since 13.10.2003
 */
@SuppressWarnings("serial")
public class ObjectOptimisticLockingFailureException extends OptimisticLockingFailureException {

	private final @Nullable Object persistentClass;

	private final @Nullable Object identifier;


	/**
	 * Create a general ObjectOptimisticLockingFailureException with the given message,
	 * without any information on the affected object.
	 * @param msg the detail message
	 * @param cause the source exception
	 */
	public ObjectOptimisticLockingFailureException(@Nullable String msg, @Nullable Throwable cause) {
		super(msg, cause);
		this.persistentClass = null;
		this.identifier = null;
	}

	/**
	 * Create a new ObjectOptimisticLockingFailureException for the given object,
	 * with the default "optimistic locking failed" message.
	 * @param persistentClass the persistent class
	 * @param identifier the ID of the object for which the locking failed
	 */
	public ObjectOptimisticLockingFailureException(Class<?> persistentClass, Object identifier) {
		this(persistentClass, identifier, null);
	}

	/**
	 * Create a new ObjectOptimisticLockingFailureException for the given object,
	 * with the default "optimistic locking failed" message.
	 * @param persistentClass the persistent class
	 * @param identifier the ID of the object for which the locking failed
	 * @param cause the source exception
	 */
	public ObjectOptimisticLockingFailureException(
			Class<?> persistentClass, Object identifier, @Nullable Throwable cause) {

		this(persistentClass, identifier,
				"Object of class [" + persistentClass.getName() + "] with identifier [" + identifier +
				"]: optimistic locking failed", cause);
	}

	/**
	 * Create a new ObjectOptimisticLockingFailureException for the given object,
	 * with the given explicit message.
	 * @param persistentClass the persistent class
	 * @param identifier the ID of the object for which the locking failed
	 * @param msg the detail message
	 * @param cause the source exception
	 */
	public ObjectOptimisticLockingFailureException(
			Class<?> persistentClass, @Nullable Object identifier, String msg, @Nullable Throwable cause) {

		super(msg, cause);
		this.persistentClass = persistentClass;
		this.identifier = identifier;
	}

	/**
	 * Create a new ObjectOptimisticLockingFailureException for the given object,
	 * with the default "optimistic locking failed" message.
	 * @param persistentClassName the name of the persistent class
	 * @param identifier the ID of the object for which the locking failed
	 */
	public ObjectOptimisticLockingFailureException(String persistentClassName, Object identifier) {
		this(persistentClassName, identifier, null);
	}

	/**
	 * Create a new ObjectOptimisticLockingFailureException for the given object,
	 * with the default "optimistic locking failed" message.
	 * @param persistentClassName the name of the persistent class
	 * @param identifier the ID of the object for which the locking failed
	 * @param cause the source exception
	 */
	public ObjectOptimisticLockingFailureException(
			String persistentClassName, Object identifier, @Nullable Throwable cause) {

		this(persistentClassName, identifier,
				"Object of class [" + persistentClassName + "] with identifier [" + identifier +
				"]: optimistic locking failed", cause);
	}

	/**
	 * Create a new ObjectOptimisticLockingFailureException for the given object,
	 * with the given explicit message.
	 * @param persistentClassName the name of the persistent class
	 * @param identifier the ID of the object for which the locking failed
	 * @param msg the detail message
	 * @param cause the source exception
	 */
	public ObjectOptimisticLockingFailureException(
			String persistentClassName, @Nullable Object identifier, @Nullable String msg, @Nullable Throwable cause) {

		super(msg, cause);
		this.persistentClass = persistentClassName;
		this.identifier = identifier;
	}


	/**
	 * Return the persistent class of the object for which the locking failed.
	 * If no Class was specified, this method returns null.
	 */
	public @Nullable Class<?> getPersistentClass() {
		return (this.persistentClass instanceof Class<?> clazz ? clazz : null);
	}

	/**
	 * Return the name of the persistent class of the object for which the locking failed.
	 * Will work for both Class objects and String names.
	 */
	public @Nullable String getPersistentClassName() {
		if (this.persistentClass instanceof Class<?> clazz) {
			return clazz.getName();
		}
		return (this.persistentClass != null ? this.persistentClass.toString() : null);
	}

	/**
	 * Return the identifier of the object for which the locking failed.
	 */
	public @Nullable Object getIdentifier() {
		return this.identifier;
	}

}
