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

package org.springframework.aop.target;

import java.io.Serializable;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import org.springframework.aop.TargetSource;
import org.springframework.util.ObjectUtils;

/**
 * Canonical {@code TargetSource} when there is no target
 * (or just the target class known), and behavior is supplied
 * by interfaces and advisors only.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public final class EmptyTargetSource implements TargetSource, Serializable {

	/** use serialVersionUID from Spring 1.2 for interoperability. */
	private static final long serialVersionUID = 3680494563553489691L;


	//---------------------------------------------------------------------
	// Static factory methods
	//---------------------------------------------------------------------

	/**
	 * The canonical (Singleton) instance of this {@link EmptyTargetSource}.
	 */
	public static final EmptyTargetSource INSTANCE = new EmptyTargetSource(null, true);


	/**
	 * Return an EmptyTargetSource for the given target Class.
	 * @param targetClass the target Class (may be {@code null})
	 * @see #getTargetClass()
	 */
	public static EmptyTargetSource forClass(@Nullable Class<?> targetClass) {
		return forClass(targetClass, true);
	}

	/**
	 * Return an EmptyTargetSource for the given target Class.
	 * @param targetClass the target Class (may be {@code null})
	 * @param isStatic whether the TargetSource should be marked as static
	 * @see #getTargetClass()
	 */
	public static EmptyTargetSource forClass(@Nullable Class<?> targetClass, boolean isStatic) {
		return (targetClass == null && isStatic ? INSTANCE : new EmptyTargetSource(targetClass, isStatic));
	}


	//---------------------------------------------------------------------
	// Instance implementation
	//---------------------------------------------------------------------

	private final @Nullable Class<?> targetClass;

	private final boolean isStatic;


	/**
	 * Create a new instance of the {@link EmptyTargetSource} class.
	 * <p>This constructor is {@code private} to enforce the
	 * Singleton pattern / factory method pattern.
	 * @param targetClass the target class to expose (may be {@code null})
	 * @param isStatic whether the TargetSource is marked as static
	 */
	private EmptyTargetSource(@Nullable Class<?> targetClass, boolean isStatic) {
		this.targetClass = targetClass;
		this.isStatic = isStatic;
	}


	/**
	 * Always returns the specified target Class, or {@code null} if none.
	 */
	@Override
	public @Nullable Class<?> getTargetClass() {
		return this.targetClass;
	}

	/**
	 * Always returns {@code true}.
	 */
	@Override
	public boolean isStatic() {
		return this.isStatic;
	}

	/**
	 * Always returns {@code null}.
	 */
	@Override
	public @Nullable Object getTarget() {
		return null;
	}


	/**
	 * Returns the canonical instance on deserialization in case
	 * of no target class, thus protecting the Singleton pattern.
	 */
	private Object readResolve() {
		return (this.targetClass == null && this.isStatic ? INSTANCE : this);
	}

	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof EmptyTargetSource that &&
				ObjectUtils.nullSafeEquals(this.targetClass, that.targetClass) &&
				this.isStatic == that.isStatic));
	}

	@Override
	public int hashCode() {
		return Objects.hash(getClass(), this.targetClass);
	}

	@Override
	public String toString() {
		return "EmptyTargetSource: " +
				(this.targetClass != null ? "target class [" + this.targetClass.getName() + "]" : "no target class") +
				", " + (this.isStatic ? "static" : "dynamic");
	}

}
