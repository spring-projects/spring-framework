/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.core;

import org.springframework.util.Assert;

/**
 * {@link InheritableThreadLocal} subclass that exposes a specified name
 * as {@link #toString()} result (allowing for introspection).
 *
 * @author Juergen Hoeller
 * @since 2.5.2
 * @see NamedThreadLocal
 */
public class NamedInheritableThreadLocal<T> extends InheritableThreadLocal<T> {

	private final String name;


	/**
	 * Create a new NamedInheritableThreadLocal with the given name.
	 * @param name a descriptive name for this ThreadLocal
	 */
	public NamedInheritableThreadLocal(String name) {
		Assert.hasText(name, "Name must not be empty");
		this.name = name;
	}

	@Override
	public String toString() {
		return this.name;
	}

}