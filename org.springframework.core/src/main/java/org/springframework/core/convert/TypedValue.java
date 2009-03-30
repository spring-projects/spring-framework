package org.springframework.core.convert;

/**
 * A value retrieved from some location such as a field access or getter invocation.
 * 
 * @author Keith Donald
 */
public class TypedValue {

	private final Object value;

	private final TypeDescriptor typeDescriptor;

	/**
	 * Creates a typed value.
	 * @param value the actual value (may be null)
	 * @param typeDescriptor the value type descriptor (may be null)
	 */
	public TypedValue(Object value, TypeDescriptor typeDescriptor) {
		this.value = value;
		this.typeDescriptor = typeDescriptor;
	}

	/**
	 * The actual value. May be null.
	 */
	public Object getValue() {
		return value;
	}

	/**
	 * Provides additional type context about the value. May be null.
	 */
	public TypeDescriptor getTypeDescriptor() {
		return typeDescriptor;
	}

}
