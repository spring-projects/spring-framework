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

package org.springframework.core;

import java.lang.reflect.Method;

import org.jspecify.annotations.Nullable;

import org.springframework.util.ObjectUtils;

/**
 * A common key class for a method against a specific target class,
 * including {@link #toString()} representation and {@link Comparable}
 * support (as suggested for custom {@code HashMap} keys as of Java 8).
 *
 * @author Juergen Hoeller
 * @since 4.3
 */
public final class MethodClassKey implements Comparable<MethodClassKey> {

	private final Method method;

	private final @Nullable Class<?> targetClass;


	/**
	 * Create a key object for the given method and target class.
	 * @param method the method to wrap (must not be {@code null})
	 * @param targetClass the target class that the method will be invoked
	 * on (may be {@code null} if identical to the declaring class)
	 */
	public MethodClassKey(Method method, @Nullable Class<?> targetClass) {
		this.method = method;
		this.targetClass = targetClass;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof MethodClassKey that &&
				this.method.equals(that.method) &&
				ObjectUtils.nullSafeEquals(this.targetClass, that.targetClass)));
	}

	@Override
	public int hashCode() {
		return this.method.hashCode() + (this.targetClass != null ? this.targetClass.hashCode() * 29 : 0);
	}

	@Override
	public String toString() {
		return this.method + (this.targetClass != null ? " on " + this.targetClass : "");
	}

	@Override
	public int compareTo(MethodClassKey other) {
		int result = this.method.getName().compareTo(other.method.getName());
		if (result == 0) {
			result = this.method.toString().compareTo(other.method.toString());
			if (result == 0 && this.targetClass != null && other.targetClass != null) {
				result = this.targetClass.getName().compareTo(other.targetClass.getName());
			}
		}
		return result;
	}

}
