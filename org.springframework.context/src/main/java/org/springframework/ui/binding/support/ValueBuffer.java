/**
 * 
 */
package org.springframework.ui.binding.support;

import org.springframework.ui.binding.support.AbstractBinding.ValueModel;

class ValueBuffer {

	private Object value;
	
	private boolean hasValue;
	
	private ValueModel model;
	
	private boolean flushFailed;
	
	private Exception flushException;
	
	public ValueBuffer(ValueModel model) {
		this.model = model;
	}
	
	public boolean hasValue() {
		return hasValue;
	}
	
	public Object getValue() {
		if (!hasValue()) {
			throw new IllegalStateException("No value in buffer");
		}
		return value;
	}
	
	public void setValue(Object value) {
		this.value = value;
		hasValue = true;
	}
	
	public void flush() {
		try {
			model.setValue(value);
			clear();
		} catch (Exception e) {
			flushFailed = true;
			flushException = e;
		}
	}

	public void clear() {
		value = null;
		hasValue = false;
		flushFailed = false;
	}
	
	public boolean flushFailed() {
		return flushFailed;
	}
	
	public Exception getFlushException() {
		return flushException;
	}
}