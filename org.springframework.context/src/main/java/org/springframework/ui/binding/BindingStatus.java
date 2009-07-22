package org.springframework.ui.binding;

/**
 * Binding states.
 * @author Keith Donald
 */
public enum BindingStatus {
	
	/**
	 * Initial state: No value is buffered, and there is a direct channel to the model value.
	 */
	CLEAN,
	
	/**
	 * An invalid source value is applied.
	 */
	INVALID_SOURCE_VALUE,
	
	/**
	 * The binding buffer contains a valid value that has not been committed.
	 */
	DIRTY,

	/**
	 * The buffered value has been committed.
	 */
	COMMITTED,
	
	/**
	 * The buffered value failed to commit.
	 */
	COMMIT_FAILURE
}