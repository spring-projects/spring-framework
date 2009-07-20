package org.springframework.ui.binding.config;

public interface Condition {
	
	boolean isTrue();
	
	static final Condition ALWAYS_TRUE = new Condition() {
		public boolean isTrue() {
			return true;
		}
	};

	static final Condition ALWAYS_FALSE = new Condition() {
		public boolean isTrue() {
			return false;
		}
	};

}
