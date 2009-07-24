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

class FieldPathElement {

	private String value;

	private boolean index;

	public FieldPathElement(String value, boolean index) {
		this.value = value;
		this.index = index;
	}

	public boolean isIndex() {
		return index;
	}

	public String getValue() {
		return value;
	}

	public int getIntValue() {
		return Integer.parseInt(value);
	}
	
	public String toString() {
		return value + ";index=" + index;
	}
}