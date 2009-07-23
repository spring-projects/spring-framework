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
package org.springframework.ui.binding.support;

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