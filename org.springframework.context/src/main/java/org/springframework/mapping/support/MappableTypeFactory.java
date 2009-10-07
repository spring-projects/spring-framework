/*
 * Copyright 2002-2009 the original author or authors.
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
package org.springframework.mapping.support;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Generic MappableTypeFactory that has no mappable types registered by default.
 * Call {@link #add(MappableType)} to register.
 * @author Keith Donald
 */
public class MappableTypeFactory {

	private Set<MappableType<?>> mappableTypes = new LinkedHashSet<MappableType<?>>();

	/**
	 * Add a MappableType to this factory.
	 * @param mappableType the mappable type
	 */
	public void add(MappableType<?> mappableType) {
		this.mappableTypes.add(mappableType);
	}

	@SuppressWarnings("unchecked")
	public <T> MappableType<T> getMappableType(T object) {
		for (MappableType type : mappableTypes) {
			if (type.isInstance(object)) {
				return type;
			}
		}
		throw new IllegalArgumentException("Object of type [" + object.getClass().getName()
				+ "] is not mappable - no suitable MappableType exists");
	}
}