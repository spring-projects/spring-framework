package org.springframework.config.java;

import java.util.List;


interface Validator {
	boolean supports(Object object);

	void validate(Object object, List<UsageError> errors);
}
