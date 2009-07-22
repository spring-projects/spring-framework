package org.springframework.ui.binding;

/**
 * Validation states.
 * @author Keith Donald
 */
public enum ValidationStatus {

	/**
	 * Initial state: No validation has run.
	 */
	NOT_VALIDATED,
	
	/**
	 * Validation has succeeded.
	 */
	VALID,
	
	/**
	 * Validation has failed.
	 */
	INVALID
}