package org.springframework.ui.binding.support;

import org.springframework.core.convert.TypeDescriptor;

/**
 * For accessing the raw bound model object.
 * @author Keith Donald
 */
public interface ValueModel {

	/**
	 * The model value.
	 */
	Object getValue();

	/**
	 * The model value type.
	 */
	Class<?> getValueType();

	/**
	 * The model value type descriptor.
	 */
	TypeDescriptor<?> getValueTypeDescriptor();

	/**
	 * If the model is writeable.
	 */
	boolean isWriteable();
	
	/**
	 * Set the model value.
	 * @throws IllegalStateException if not writeable
	 */
	void setValue(Object value);
}