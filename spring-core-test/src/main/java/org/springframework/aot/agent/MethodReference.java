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

package org.springframework.aot.agent;

import java.util.Objects;

import org.springframework.lang.Nullable;

/**
 * Reference to a Java method, identified by its owner class and the method name.
 *
 * <p>This implementation is ignoring parameters on purpose, as the goal here is
 * to inform developers on invocations requiring additional
 * {@link org.springframework.aot.hint.RuntimeHints} configuration, not
 * precisely identifying a method.
 *
 * @author Brian Clozel
 * @since 6.0
 */
public final class MethodReference {

	private final String className;

	private final String methodName;

	private MethodReference(String className, String methodName) {
		this.className = className;
		this.methodName = methodName;
	}

	public static MethodReference of(Class<?> klass, String methodName) {
		return new MethodReference(klass.getCanonicalName(), methodName);
	}

	/**
	 * Return the declaring class for this method.
	 * @return the declaring class name
	 */
	public String getClassName() {
		return this.className;
	}

	/**
	 * Return the name of the method.
	 * @return the method name
	 */
	public String getMethodName() {
		return this.methodName;
	}

	@Override
	public boolean equals(@Nullable Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		MethodReference that = (MethodReference) o;
		return this.className.equals(that.className) && this.methodName.equals(that.methodName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.className, this.methodName);
	}

	@Override
	public String toString() {
		return this.className + '#' + this.methodName;
	}
}
