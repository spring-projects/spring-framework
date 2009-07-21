/**
 * 
 */
package org.springframework.ui.binding.support;

public class PropertyPathElement {

	private String value;

	private boolean index;

	public PropertyPathElement(String value, boolean index) {
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