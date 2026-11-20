/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
