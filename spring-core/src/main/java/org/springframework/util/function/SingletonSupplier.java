/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.util.function;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * A {@link java.util.function.Supplier} decorator that caches a singleton result and
 * makes it available from {@link #get()} (nullable) and {@link #obtain()} (null-safe).
 *
 * <p>A {@code SingletonSupplier} can be constructed via {@code of} factory methods
 * or via constructors that provide a default supplier as a fallback. This is
 * particularly useful for method reference suppliers, falling back to a default
 * supplier for a method that returned {@code null} and caching the result.
 *
 * @author Juergen Hoeller
 * @author Yanming Zhou
 * @since 5.1
 * @param <T> the type of results supplied by this supplier
 */
public class SingletonSupplier<T> implements Supplier<T> {

	@Nullable
	private final Supplier<? extends T> instanceSupplier;

	@Nullable
	private final Supplier<? extends T> defaultSupplier;

	@Nullable
	private volatile T singletonInstance;

	/**
	 * Guards access to write operations on the {@code singletonInstance} field.
	 */
	private final Lock writeLock = new ReentrantLock();


	/**
	 * Build a {@code SingletonSupplier} with the given singleton instance
	 * and a default supplier for the case when the instance is {@code null}.
	 * @param instance the singleton instance (potentially {@code null})
	 * @param defaultSupplier the default supplier as a fallback
	 */
	public SingletonSupplier(@Nullable T instance, Supplier<? extends T> defaultSupplier) {
		this.instanceSupplier = null;
		this.defaultSupplier = defaultSupplier;
		this.singletonInstance = instance;
	}

	/**
	 * Build a {@code SingletonSupplier} with the given instance supplier
	 * and a default supplier for the case when the instance is {@code null}.
	 * @param instanceSupplier the immediate instance supplier
	 * @param defaultSupplier the default supplier as a fallback
	 */
	public SingletonSupplier(@Nullable Supplier<? extends T> instanceSupplier, Supplier<? extends T> defaultSupplier) {
		this.instanceSupplier = instanceSupplier;
		this.defaultSupplier = defaultSupplier;
	}

	private SingletonSupplier(Supplier<? extends T> supplier) {
		this.instanceSupplier = supplier;
		this.defaultSupplier = null;
	}

	private SingletonSupplier(T singletonInstance) {
		this.instanceSupplier = null;
		this.defaultSupplier = null;
		this.singletonInstance = singletonInstance;
	}


	/**
	 * Get the shared singleton instance for this supplier.
	 * @return the singleton instance (or {@code null} if none)
	 */
	@Override
	@Nullable
	public T get() {
		T instance = this.singletonInstance;
		if (instance == null) {
			this.writeLock.lock();
			try {
				instance = this.singletonInstance;
				if (instance == null) {
					if (this.instanceSupplier != null) {
						instance = this.instanceSupplier.get();
					}
					if (instance == null && this.defaultSupplier != null) {
						instance = this.defaultSupplier.get();
					}
					this.singletonInstance = instance;
				}
			}
			finally {
				this.writeLock.unlock();
			}
		}
		return instance;
	}

	/**
	 * Obtain the shared singleton instance for this supplier.
	 * @return the singleton instance (never {@code null})
	 * @throws IllegalStateException in case of no instance
	 */
	public T obtain() {
		T instance = get();
		Assert.state(instance != null, "No instance from Supplier");
		return instance;
	}


	/**
	 * Build a {@code SingletonSupplier} with the given singleton instance.
	 * @param instance the singleton instance (never {@code null})
	 * @return the singleton supplier (never {@code null})
	 */
	public static <T> SingletonSupplier<T> of(T instance) {
		return new SingletonSupplier<>(instance);
	}

	/**
	 * Build a {@code SingletonSupplier} with the given singleton instance.
	 * @param instance the singleton instance (potentially {@code null})
	 * @return the singleton supplier, or {@code null} if the instance was {@code null}
	 */
	@Nullable
	public static <T> SingletonSupplier<T> ofNullable(@Nullable T instance) {
		return (instance != null ? new SingletonSupplier<>(instance) : null);
	}

	/**
	 * Build a {@code SingletonSupplier} with the given supplier.
	 * @param supplier the instance supplier (never {@code null})
	 * @return the singleton supplier (never {@code null})
	 */
	public static <T> SingletonSupplier<T> of(Supplier<T> supplier) {
		return new SingletonSupplier<>(supplier);
	}

	/**
	 * Build a {@code SingletonSupplier} with the given supplier.
	 * @param supplier the instance supplier (potentially {@code null})
	 * @return the singleton supplier, or {@code null} if the instance supplier was {@code null}
	 */
	@Nullable
	public static <T> SingletonSupplier<T> ofNullable(@Nullable Supplier<T> supplier) {
		return (supplier != null ? new SingletonSupplier<>(supplier) : null);
	}

}
