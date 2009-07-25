/*
 * Copyright 2004-2009 the original author or authors.
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
package org.springframework.model.ui.support;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * A type descriptor for a parameterizable collection type such as a java.util.List&lt;?&gt;.
 * @author Keith Donald
 * @since 3.0
 */
public class CollectionTypeDescriptor {

	private Class<?> type;

	private Class<?> elementType;

	public CollectionTypeDescriptor(Class<?> type, Class<?> elementType) {
		Assert.notNull(type, "The collection type is required");
		this.type = type;
		this.elementType = elementType;
	}

	/**
	 * The collection type.
	 */
	public Class<?> getType() {
		return type;
	}

	/**
	 * The parameterized collection element type.
	 */
	public Class<?> getElementType() {
		return elementType;
	}

	public boolean equals(Object o) {
		if (!(o instanceof CollectionTypeDescriptor)) {
			return false;
		}
		CollectionTypeDescriptor type = (CollectionTypeDescriptor) o;
		return type.equals(type.type)
				&& ObjectUtils.nullSafeEquals(elementType, type.elementType);
	}

	public int hashCode() {
		return type.hashCode() + elementType.hashCode();
	}
}