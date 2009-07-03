package org.springframework.ui.validation;

import org.springframework.ui.alert.Alert;

public interface ValidationResult {

	/**
	 * The name of the model property associated with this validation result.
	 */
	String getProperty();
	
	/**
	 * Indicates if the validation failed.
	 */
	boolean isFailure();

	/**
	 * Gets the alert for this validation result, appropriate for rendering the result to the user.
	 * An alert describing a successful validation will have info severity.
	 * An alert describing a failed validation will have either warning or error severity.
	 */
	Alert getAlert();
}
