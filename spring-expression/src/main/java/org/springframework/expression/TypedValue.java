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

package org.springframework.expression;

import org.jspecify.annotations.Nullable;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.util.ObjectUtils;

/**
 * Encapsulates an object and a {@link TypeDescriptor} that describes it.
 *
 * <p>The type descriptor can contain generic declarations that would not
 * be accessible through a simple {@code getClass()} call on the object.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @since 3.0
 */
public class TypedValue {

	/**
	 * {@link TypedValue} for {@code null}.
	 */
	public static final TypedValue NULL = new TypedValue(null);


	private final @Nullable Object value;

	private @Nullable TypeDescriptor typeDescriptor;


	/**
	 * Create a {@link TypedValue} for a simple object. The {@link TypeDescriptor}
	 * is inferred from the object, so no generic declarations are preserved.
	 * @param value the object value
	 */
	public TypedValue(@Nullable Object value) {
		this.value = value;
		this.typeDescriptor = null;  // initialized when/if requested
	}

	/**
	 * Create a {@link TypedValue} for a particular value with a particular
	 * {@link TypeDescriptor} which may contain additional generic declarations.
	 * @param value the object value
	 * @param typeDescriptor a type descriptor describing the type of the value
	 */
	public TypedValue(@Nullable Object value, @Nullable TypeDescriptor typeDescriptor) {
		this.value = value;
		this.typeDescriptor = typeDescriptor;
	}


	public @Nullable Object getValue() {
		return this.value;
	}

	public @Nullable TypeDescriptor getTypeDescriptor() {
		if (this.typeDescriptor == null && this.value != null) {
			this.typeDescriptor = TypeDescriptor.forObject(this.value);
		}
		return this.typeDescriptor;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		// Avoid TypeDescriptor initialization if not necessary
		return (this == other || (other instanceof TypedValue that &&
				ObjectUtils.nullSafeEquals(this.value, that.value) &&
				((this.typeDescriptor == null && that.typeDescriptor == null) ||
						ObjectUtils.nullSafeEquals(getTypeDescriptor(), that.getTypeDescriptor()))));
	}

	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHashCode(this.value);
	}

	@Override
	public String toString() {
		return "TypedValue: '" + this.value + "' of [" + getTypeDescriptor() + "]";
	}

}
